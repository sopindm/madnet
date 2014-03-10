(ns madnet.channel.pipe
  (:require [madnet.event :as e])
  (:require [madnet.channel.events :as ce])
  (:import [madnet.channel IChannel Result Events]))

(deftype PipeReader [^java.nio.channels.Channel ch events]
  IChannel
  (close [this] (.close (:on-read events)) (.close ch)
    (e/emit! (:on-close events)))
  (isOpen [this] (.isOpen ch))
  (clone [this] this)
  (events [this] events)
  (register [this set]
    (e/conj! set (:on-read events))
    (e/emit! (:on-read events)))
  (read [this channel]
    (when-not (.isOpen this)
      (throw (java.nio.channels.ClosedChannelException.)))
    (when (instance? madnet.range.nio.ByteRange channel)
      (let [buffer (.buffer channel)
            begin (.position buffer)]
        (when (neg? (.read ch (.buffer channel)))
          (.close this))
        (Result. (- (.position buffer) begin)
                 (- (.position buffer) begin))))))

(deftype PipeWriter [ch events]
  IChannel
  (close [this] (.close (:on-write events)) (.close ch)
    (e/emit! (:on-close events)))
  (isOpen [this] (.isOpen ch))
  (clone [this] this)
  (events [this] events)
  (register [this set]
    (e/conj! set (-> this .events .onWrite))
    (e/emit! (:on-write events)))
  (write [this channel]
    (when-not (.isOpen this)
      (throw (java.nio.channels.ClosedChannelException.)))
    (when (instance? madnet.range.nio.ByteRange channel)
      (let [buffer (.buffer channel)
            begin (.position buffer)]
        (try (.write ch (.buffer channel))
             (catch java.io.IOException e
               (.close this)))
        (Result. (- (.position buffer) begin)
                 (- (.position buffer) begin))))))

(deftype Pipe [reader writer events]
  IChannel
  (close [this] (.close reader) (.close writer))
  (isOpen [this] (or (.isOpen reader) (.isOpen writer)))
  (clone [this] this)
  (events [this] events)
  (register [this set] (.register reader set) (.register writer set))
  (write [this channel] (.write writer channel))
  (read [this channel] (.read reader channel)))

(defn- pipe-reader- [pipe]
  (let [on-read (e/selector (.source pipe) :read)
        on-close (e/event () :one-shot)]
    (.configureBlocking (.source pipe) false)
    (let [reader (PipeReader. (.source pipe)
                              (ce/events :on-read on-read
                                         :on-close on-close))]
      (e/attach! on-read reader)
      (e/attach! on-close reader)
      reader)))

(defn- pipe-writer- [pipe]
  (let [on-write (e/selector (.sink pipe) :write)
        on-close (e/event () :one-shot)]
    (.configureBlocking (.sink pipe) false)    
    (let [writer (PipeWriter. (.sink pipe)
                              (ce/events :on-write on-write
                                         :on-close on-close))]
      (e/attach! on-write writer)
      (e/attach! on-close writer)
      writer)))

(defn pipe ^madnet.channel.pipe.Pipe []
  (let [pipe (java.nio.channels.Pipe/open)
        reader (pipe-reader- pipe)
        writer (pipe-writer- pipe)
        on-close (e/when-every (:on-close (.events reader))
                               (:on-close (.events writer)))]
    (let [pipe (Pipe. reader writer
                      (ce/events :on-read (-> reader .events :on-read)
                                 :on-write (-> writer .events :on-write)
                                 :on-close on-close))]
      (e/attach! (-> pipe .events :on-close) pipe)
      pipe)))
          
      



