(ns madnet.channel.events
  (:import [madnet.channel IEvents]))

(defrecord Events [on-read on-write on-close]
  IEvents
  (onRead [this] on-read)
  (onWrite [this] on-write)
  (onClose [this] on-close))

(defn events [& {:keys [on-read on-write on-close]}]
  (Events. on-read on-write on-close))
