(ns madnet.channel.pipe
  (:require [madnet.event :as e])
  (:require [madnet.channel.events :as ce])
  (:import [madnet.channel IChannel Result Events]
           [madnet.channel WritableChannel ReadableChannel]
           [madnet.channel ObjectReader ObjectWriter]))
            

(deftype Pipe [reader writer events]
  IChannel
  (close [this] (.close reader) (.close writer))
  (isOpen [this] (or (.isOpen reader) (.isOpen writer)))
  (clone [this] this)
  (events [this] events)
  (register [this set] (.register reader set) (.register writer set))
  (push [this obj] (.push writer obj))
  (push [this obj timeout] (.push writer obj timeout))
  (tryPush [this obj] (.tryPush writer obj))
  (pop [this] (.pop reader))
  (pop [this timeout] (.pop reader timeout))
  (tryPop [this] (.tryPop reader))
  (write [this channel] (.write writer channel))
  (read [this channel] (.read reader channel)))

(defn pipe ^madnet.channel.pipe.Pipe []
  (let [pipe (java.nio.channels.Pipe/open)
        reader (ReadableChannel. (.source pipe))
        writer (WritableChannel. (.sink pipe))
        on-close (e/when-every (-> reader .events .onClose)
                               (-> writer .events .onClose))]
    (let [pipe (Pipe. reader writer
                      (ce/events :on-read (-> reader .events .onRead)
                                 :on-write (-> writer .events .onWrite)
                                 :on-close on-close))]
      (e/attach! (-> pipe .events :on-close) pipe)
      pipe)))

(defn- object-wire
  ([] (object-wire nil))
  ([size]
     (let [current-size (atom 0)
           deque (java.util.concurrent.ConcurrentLinkedDeque.)]
       (proxy [madnet.channel.ObjectWire] []
         (offer []
           (if (or (nil? size) (< @current-size size))
             (do (swap! current-size inc) true)
             false))
         (cancelOffer [] (swap! current-size dec) nil)
         (commitOffer [obj] (.add deque obj))
         (fetch [] (.poll deque))
         (cancelFetch [obj] (.addFirst deque obj))
         (commitFetch [] (swap! current-size dec))))))

(defn- object-pipe- [wire]
  (let [reader (ObjectReader. wire)
        writer (ObjectWriter. wire)
        on-close (e/when-every (-> reader .events .onClose)
                               (-> writer .events .onClose))]
    (let [pipe (Pipe. reader writer
                      (ce/events :on-read (-> reader .events .onRead)
                                 :on-write (-> writer .events .onWrite)
                                 :on-close on-close))]
      (e/attach! (-> pipe .events :on-close) pipe)
      pipe)))

(defn object-pipe ^madnet.channel.pipe.Pipe
  ([] (object-pipe- (object-wire)))
  ([size] (object-pipe- (object-wire size))))
      



