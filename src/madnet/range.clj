(ns madnet.range
  (:refer-clojure :exclude [take drop sequence take-last drop-last read range proxy])
  (:import [madnet.range Range ProxyRange ObjectRange]))

;;
;; Range operation wrappers
;;

(defn- proxy-method- [option]
  (case option
    :read-only '(write [_] (throw (UnsupportedOperationException.)))
    :write-only '(read [_] (throw (UnsupportedOperationException.)))
    option))

(defmacro proxy [range & options-and-methods]
  (if (empty? options-and-methods)
    `(ProxyRange. ~range)
    `(clojure.core/proxy [ProxyRange] [~range]
       ~@(map proxy-method- options-and-methods))))

(defn size [range]
  (.size range))

(defn take! [n range]
  (.take range n))

(defn take-last! [n range]
  (.takeLast range n))

(defn drop! [n range]
  (.drop range n))

(defn drop-last! [n range]
  (.dropLast range n))

(defn expand! [n range]
  (.expand range n))

(declare take)
(defn split! [n range]
  (let [split (take n range)]
    (drop! n range)
    split))

;;
;; Immutable range operations
;;

(defn take [n range]
  (take! n (.clone range)))

(defn take-last [n range]
  (take-last! n (.clone range)))

(defn drop [n range]
  (drop! n (.clone range)))

(defn drop-last [n range]
  (drop-last! n (.clone range)))

(defn expand [n range]
  (expand! n (.clone range)))

(defn split [n range]
  (let [clone (.clone range)]
    [(split! n clone) clone]))

;;
;; Reading/writing
;;

(defn write! [dest src]
  (or (.write dest src) (.read src dest) (throw (UnsupportedOperationException.)))
  dest)

(defn read! [dest src]
  (or (.read dest src) (.write src dest) (throw (UnsupportedOperationException.)))
  dest)

(defn write [dest src]
  (let [writen (.clone dest)
        read (.clone src)]
    (write! writen read)
    [writen read]))

(defn read [dest src]
  (let [read (.clone dest)
        writen (.clone src)]
    (read! read writen)
    [read writen]))

;;
;; Buffers
;;

(deftype Buffer [generator size]
  clojure.lang.Seqable
  (seq [this] (seq (generator 0 size)))
  clojure.lang.Counted
  (count [this] size)
  clojure.lang.IFn
  (invoke [this] (generator 0 size))
  (invoke [this end] (generator 0 end))
  (invoke [this begin end] (generator begin end)))

(defn- object-range-generator [size]
  (let [coll (java.util.ArrayList. size)]
    (dotimes [i size]
      (.add coll nil))
    (fn [begin end] (ObjectRange. begin end coll))))

(defn- byte-range-generator [size options]
  (let [b (java.nio.ByteBuffer/allocate size)]
    (fn [begin end] (madnet.range.nio.ByteRange. begin end b))))

(defn- char-range-generator [size options]
  (let [b (java.nio.CharBuffer/allocate size)]
    (fn [begin end] (madnet.range.nio.CharRange. begin end b))))

(defn- circular-generator [generator size]
  (fn [begin end] (madnet.range.CircularRange.
                   (generator begin end)
                   (madnet.range.IntegerRange. 0 size))))

(defn buffer [size & options]
  (let [{:keys [element] :as options} (apply sorted-map options)
        generator (case element
                    (:object nil) (object-range-generator size)
                    :byte (byte-range-generator size options)
                    :char (char-range-generator size options))]
    (Buffer. (if (get options :circular true)
               (circular-generator generator size)
               generator)
             size)))

(defn circular? [buffer]
  (isa? (type (buffer)) madnet.range.CircularRange))
