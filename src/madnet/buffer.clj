(ns madnet.buffer
  (:import [madnet.range ObjectRange]
           [java.nio ByteBuffer CharBuffer]))

(deftype Buffer [generator size]
  clojure.lang.Seqable
  (seq [this] (seq (generator 0 size)))
  clojure.lang.Counted
  (count [this] size)
  clojure.lang.IFn
  (invoke [this] (generator 0 size))
  (invoke [this end] (generator 0 end))
  (invoke [this begin end] (generator begin end)))

(defn- check-no-options [options keys]
  (if (some #(keys (first %)) options)
    (throw (IllegalArgumentException.))))

(defn- check-options [options keys]
  (if (some #(not (keys (first %))) options)
    (throw (IllegalArgumentException.))))

(defn- object-range-generator [options]
  (check-options options #{})
  {:buffer (fn [size]
             (let [buffer (java.util.ArrayList. size)]
               (dotimes [i size] (.add buffer nil))
               buffer))
   :wrap (fn [coll] (java.util.ArrayList. coll))
   :range (fn [buffer begin end] (ObjectRange. begin end buffer))})
  
(defn- byte-range-generator [{:keys [direct] :as options}]
  (check-options options #{:direct})
  {:buffer (fn [size] (if direct (ByteBuffer/allocateDirect size)
                          (ByteBuffer/allocate size)))
   :wrap (fn [coll]
           (check-options options #{}) 
           (ByteBuffer/wrap coll))
   :range (fn [buffer begin end]
            (madnet.range.nio.ByteRange. begin end buffer))})

(defn- char-range-generator [options]
  (check-options options #{})
  {:buffer (fn [size] (CharBuffer/allocate size))
   :wrap (fn [coll] (CharBuffer/wrap coll))
   :range (fn [buffer begin end]
            (madnet.range.nio.CharRange. begin end buffer))})

(defn- circular-generator [generator size]
  (assoc generator 
    :range (fn [buffer begin end]
             (madnet.range.CircularRange.
              ((:range generator) buffer begin end)
              (madnet.range.IntegerRange. 0 size)))))

(defn- primitive-generator [options]
  (let [element (get options :element :object)
        options (dissoc options :element)]
    (case element 
      :object (object-range-generator options)
      :byte (byte-range-generator options)
      :char (char-range-generator options))))

(defn- generator [size options]
  (let [options (apply sorted-map options)
        circular? (get options :circular false)
        options (dissoc options :circular)
        generator (primitive-generator options)]
    (if circular? (circular-generator generator size) generator)))

(defn buffer [size & options]
  (let [generator (generator size options)
        coll ((:buffer generator) size)
        range-generator (:range generator)]
    (Buffer. (fn [begin end] (range-generator coll begin end)) size)))

(defn element-type [coll]
  (let [type (type coll)]
    (cond 
     (isa? type (Class/forName "[B")) :byte
     (isa? type (Class/forName "[C")) :char
     :else :object)))

(defn wrap [coll & options]
  (check-no-options (apply sorted-map options) #{:element})
  (let [element (element-type coll)
        options (concat [:element element] options)
        size (count coll)
        generator (generator size options)
        coll ((:wrap generator) coll)
        range-generator (:range generator)]
    (Buffer. (fn [begin end] (range-generator coll begin end)) size)))

(defn circular? [buffer]
  (isa? (type (buffer)) madnet.range.CircularRange))

(defn direct? [buffer]
  (let [range (buffer)
        linear (if (circular? buffer) (.first range) range)]
    (and (isa? (type linear) madnet.range.nio.Range)
         (.isDirect (.buffer linear)))))
