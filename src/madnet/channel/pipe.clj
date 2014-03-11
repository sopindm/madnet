(ns madnet.channel.pipe
  (:require [madnet.event :as e])
  (:require [madnet.channel.events :as ce])
  (:import [madnet.channel IChannel Result Events]
           [madnet.channel.pipe PipeReader PipeWriter]))

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
        reader (PipeReader. pipe)
        writer (PipeWriter. pipe)
        on-close (e/when-every (-> reader .events .onClose)
                               (-> writer .events .onClose))]
    (let [pipe (Pipe. reader writer
                      (ce/events :on-read (-> reader .events .onRead)
                                 :on-write (-> writer .events .onWrite)
                                 :on-close on-close))]
      (e/attach! (-> pipe .events :on-close) pipe)
      pipe)))
          
      



