(ns madnet.sequence
  (:refer-clojure :exclude [sequence])
  (:require [madnet.channel :as c]
            [madnet.range :as r]
            [madnet.buffer :as b]))

(declare size)
(deftype Sequence [buffer reader writer circular?]
  clojure.lang.Seqable
  (seq [this] (seq (.reader this)))
  clojure.lang.Counted
  (count [this] (size this))
  madnet.channel.IChannel
  (clone [this] (Sequence. buffer (.clone reader) (.clone writer) circular?))
  (close [this] nil)
  (isOpen [this] true)
  (push [this obj] (.push writer obj) (.expand reader 1))
  (push [this obj timeout] (boolean (when (.push writer obj timeout) (.expand reader 1) this)))
  (tryPush [this obj] (boolean (when (.tryPush writer obj) (.expand reader 1) true)))
  (pop [this] (let [result (.pop reader)]
                (when circular? (.expand writer 1)) result))
  (pop [this timeout] (when-let [result (.pop reader timeout)]
                        (when circular? (.expand writer 1)) result))
  (tryPop [this] (when-let [result (.tryPop reader)]
                   (when circular? (.expand writer 1)) result))
  (write [this channel]
    (when-let [result (or (.write writer channel) (.read channel writer))]
      (.expand reader (.read result))
      result))
  (read [this channel]
    (let [reader (.reader this)]
      (when-let [result (or (.read reader channel) (.write channel reader))]
        (when circular? (.expand writer (.writen result)))
        result))))

(defn- buffer- [buffer-or-spec]
  (let [size (if (sequential? buffer-or-spec)
               (first buffer-or-spec)
               buffer-or-spec)
        options (apply sorted-map (if (sequential? buffer-or-spec)
                                    (rest buffer-or-spec)))]
    (b/buffer size (assoc options :circular
                          (get options :circular true)))))

(defn ranges [buffer reader-option writer-option]
  (letfn [(parse-option [[begin end]]
            (if (<= begin end) [begin end 0] [begin (count buffer) end]))
          (buffer- [begin end tail](r/expand! tail (buffer begin end)))
          (before-range [[begin end tail] buffer]
            (if (b/circular? buffer) 
              (if (zero? tail) [end (count buffer) begin] [tail begin 0])
              [0 begin 0]))
          (after-range [[begin end tail] buffer]
            (if (b/circular? buffer)
              (before-range [begin end tail] buffer)
              [end (count buffer) 0]))]
    (let [reader-option (if reader-option (parse-option reader-option))
          writer-option (if writer-option (parse-option writer-option))
          writer (or writer-option [0 (count buffer) 0])
          reader (or reader-option (before-range writer buffer))
          writer (or writer-option (after-range reader buffer))]
      [(apply buffer- reader) (apply buffer- writer)])))

(defn sequence [buffer-or-spec & {:as options}]
  (let [b (if (isa? (type buffer-or-spec) madnet.buffer.Buffer)
            buffer-or-spec
            (buffer- buffer-or-spec))
        [reader writer] (ranges b (:reader options) (:writer options))]
    (Sequence. b reader writer (b/circular? b))))

(defn wrap [coll]
  (let [b (b/wrap coll)]
    (Sequence. b (b 0 (count b)) (b (count b) (count b)) false)))

(defn reader [seq]
  (.reader seq))

(defn writer [seq]
  (.writer seq))

(defn size [seq]
  (r/size (.reader seq)))

(defn free [seq]
  (r/size (.writer seq)))

(defn circular? [seq]
  (.circular? seq))
