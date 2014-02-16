(ns madnet.sequence
  (:require [madnet.channel :as c]
            [madnet.range :as r]
            [madnet.buffer :as b])
  (:import [madnet.range LinkedRange]))

(declare size)
(deftype Sequence [buffer reader writer]
  clojure.lang.Seqable
  (seq [this] (seq (.reader this)))
  clojure.lang.Counted
  (count [this] (size this))
  madnet.channel.IChannel
  (write [this channel]
    (let [writer (.writer this)]
      (or (.write writer channel) (.read channel writer))))
  (read [this channel]
    (let [reader (.reader this)]
      (or (.read reader channel) (.write channel reader)))))

(defn- buffer- [buffer-or-spec]
  (let [size (if (sequential? buffer-or-spec)
               (first buffer-or-spec)
               buffer-or-spec)
        options (apply sorted-map (if (sequential? buffer-or-spec)
                                    (rest buffer-or-spec)))]
    (b/buffer size (assoc options :circular
                          (get options :circular true)))))

(defn ranges [buffer reader-option writer-option]
  (letfn [(buffer- [begin end] (buffer begin end))
          (before-range [[begin end] buffer]
            (if (b/circular? buffer) [end begin] [0 begin]))
          (after-range [[begin end] buffer]
            (if (b/circular? buffer) [end begin] [end (count buffer)]))]
    (let [writer (or writer-option [0 (count buffer)])
          reader (or reader-option (before-range writer buffer))
          writer (or writer-option (after-range reader buffer))]
      [(apply buffer- reader) (apply buffer- writer)])))

(defn sequence [buffer-or-spec & {:as options}]
  (let [b (if (isa? (type buffer-or-spec) madnet.buffer.Buffer)
            buffer-or-spec
            (buffer- buffer-or-spec))
        [reader writer] (ranges b (:reader options) (:writer options))]
    (Sequence. b (LinkedRange. reader nil writer)
               (LinkedRange. writer reader nil))))

(defn reader [seq]
  (.reader seq))

(defn writer [seq]
  (.writer seq))

(defn size [seq]
  (r/size (.reader seq)))

(defn free [seq]
  (r/size (.writer seq)))

(defn circular? [seq]
  (b/circular? (.buffer seq)))
