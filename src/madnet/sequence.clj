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
      (if (or (.write writer channel) (.read channel writer)) this)))
  (read [this channel]
    (let [reader (.reader this)]
      (if (or (.read reader channel) (.write channel reader)) this))))

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
          (before-range [[begin end] buffer] [0 begin])
          (after-range [[begin end] buffer] [end (count buffer)])]
    (let [reader (or reader-option [0 0])
          writer (or writer-option (after-range reader buffer))
          reader (or reader-option (before-range writer buffer))]
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
