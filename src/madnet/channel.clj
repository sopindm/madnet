(ns madnet.channel
  (:refer-clojure :exclude [read])
  (:require [madnet.channel.pipe])
  (:import [madnet.channel IChannel]
           [madnet.event ISignalSet]))

(defn register [^IChannel ch ^ISignalSet set]
  (.register ch set))

(defn events [^IChannel ch]
  (.events ch))

;;
;; Reading/writing
;;

(defn readable? [^IChannel ch]
  (.readable ch))

(defn writeable? [^IChannel ch]
  (.writeable ch))

(defn write! [^IChannel dest ^IChannel src]
  (or (.write dest src) (.read src dest) (throw (UnsupportedOperationException.))))

(defn read! [^IChannel dest ^IChannel src]
  (or (.read dest src) (.write src dest) (throw (UnsupportedOperationException.))))

(defn write [^IChannel dest ^IChannel src]
  (let [writen (.clone dest)
        read (.clone src)]
    (write! writen read)
    [writen read]))

(defn read [^IChannel dest ^IChannel src]
  (let [read (.clone dest)
        writen (.clone src)]
    (read! read writen)
    [read writen]))

(defn close! [^IChannel channel & options]
  (when (or (some #{:write} options) (empty? options))
    (.closeWrite channel))
  (when (or (some #{:read} options) (empty? options))
    (.closeRead channel))
  channel)

(def pipe madnet.channel.pipe/pipe)

