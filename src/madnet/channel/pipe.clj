(ns madnet.channel.pipe
  (:require [madnet.event :as e])
  (:import [madnet.channel IChannel Result Events]))


(deftype PipeReader [^java.nio.channels.Channel ch events]
  java.io.Closeable
  (close [this] (.closeRead this))
  IChannel
  (clone [this] this)
  (events [this] events)
  (register [this set]
    (e/conj! set (.onRead events)))
  (readable [this] (.isOpen ch))
  (closeRead [this]
    (e/start! (.onReadClosed events))
    (.close (.onReadClosed events))
    (.close ch))
  (writeable [this] false)
  (closeWrite [this] this)
  (read [this channel]
    (when (instance? madnet.range.nio.ByteRange channel)
      (let [buffer (.buffer channel)
            begin (.position buffer)]
        (.read ch (.buffer channel))
        (Result. (- (.position buffer) begin)
                 (- (.position buffer) begin))))))

(deftype PipeWriter [ch events]
  java.io.Closeable
  (close [this] (.closeWrite this))
  IChannel
  (clone [this] this)
  (events [this] events)
  (register [this set]
    (e/conj! set (-> this .events .onWrite)))
  (readable [this] false)
  (closeRead [this] this)
  (writeable [this] (.isOpen ch))
  (closeWrite [this]
    (e/start! (.onWriteClosed events))
    (.close (.onWriteClosed events))
    (.close ch))
  (write [this channel]
    (when (instance? madnet.range.nio.ByteRange channel)
      (let [buffer (.buffer channel)
            begin (.position buffer)]
        (.write ch (.buffer channel))
        (Result. (- (.position buffer) begin)
                 (- (.position buffer) begin))))))

(deftype Pipe [reader writer events]
  java.io.Closeable
  (close [this]
    (e/start! (.onClosed events))
    (.close (.onClosed events))
    (.close reader) (.close writer))
  IChannel
  (clone [this] this)
  (events [this] events)
  (register [this set]
    (.register reader set)
    (.register writer set))
  (readable [this] (.readable reader))
  (closeRead [this] (.closeRead reader))
  (writeable [this] (.writeable writer))
  (closeWrite [this] (.closeWrite writer))
  (write [this channel]
    (.write writer channel))
  (read [this channel]
    (.read reader channel)))

(defn pipe ^madnet.channel.pipe.Pipe []
  (let [pipe (java.nio.channels.Pipe/open)
        on-read (e/selector (.source pipe) :read)
        on-write (e/selector (.sink pipe) :write)
        on-read-closed (e/flash)
        on-write-closed (e/flash)
        on-closed (e/flash)]
    (.configureBlocking (.sink pipe) false)
    (.configureBlocking (.source pipe) false)
    (Pipe. (PipeReader. (.source pipe)
                        (Events. on-read
                                 nil
                                 on-read-closed
                                 nil
                                 on-read-closed))
           (PipeWriter. (.sink pipe)
                        (Events. nil
                                 on-write
                                 nil
                                 on-write-closed
                                 on-write-closed))
           (Events. on-read
                    on-write
                    on-read-closed
                    on-write-closed
                    on-closed))))


