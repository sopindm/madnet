(ns madnet.channel
  (:refer-clojure :exclude [read pop!])
  (:require [madnet.channel.pipe])
  (:import [madnet.channel IChannel]
           [madnet.event ISignalSet]))

(defn open? [^IChannel ch]
  (.isOpen ch))

(defn register [^IChannel ch ^ISignalSet set]
  (.register ch set))

(defn events [^IChannel ch]
  (.events ch))

;;
;; Reading/writing
;;

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

(defn push! [ch obj & {timeout :timeout}]
  (letfn [(try-push- [] (if (.tryPush ch obj) ch))
          (push- [] (do (.push ch obj) ch))
          (push-with-timeout- [] (if (.push ch obj timeout) ch))]
    (if timeout (if (zero? timeout) (try-push-) (push-with-timeout-))
      (push-))))

(defn pop! [ch & args] (.pop ch) ch)

(defn peek! [ch & args] (.peek ch) ch)

;;
;; Wrappers
;;

(def pipe madnet.channel.pipe/pipe)

