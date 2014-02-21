(ns madnet.channel
  (:refer-clojure :exclude [read])
  (:import [madnet.channel IChannel Result]))

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

;;
;; Pipes
;;

(deftype PipeReader [^java.nio.channels.Channel ch]
  java.io.Closeable
  (close [this] (.close ch))
  IChannel
  (clone [this] this)
  (readable [this] (.isOpen ch))
  (closeRead [this] (.close ch))
  (writeable [this] false)
  (closeWrite [this] this)
  (read [this channel]
    (when (instance? madnet.range.nio.ByteRange channel)
      (let [buffer (.buffer channel)
            begin (.position buffer)]
        (.read ch (.buffer channel))
        (Result. (- (.position buffer) begin)
                 (- (.position buffer) begin))))))

(deftype PipeWriter [ch]
  java.io.Closeable
  (close [this] (.close ch))
  IChannel
  (clone [this] this)
  (readable [this] false)
  (closeRead [this] this)
  (writeable [this] (.isOpen ch))
  (closeWrite [this] (.close ch))
  (write [this channel]
    (when (instance? madnet.range.nio.ByteRange channel)
      (let [buffer (.buffer channel)
            begin (.position buffer)]
        (.write ch (.buffer channel))
        (Result. (- (.position buffer) begin)
                 (- (.position buffer) begin))))))

(deftype Pipe [reader writer]
  java.io.Closeable
  (close [this] (.close reader) (.close writer))
  IChannel
  (clone [this] this)
  (readable [this] (.readable reader))
  (closeRead [this] (.closeRead reader))
  (writeable [this] (.writeable writer))
  (closeWrite [this] (.closeWrite writer))
  (write [this channel]
    (.write writer channel))
  (read [this channel]
    (.read reader channel)))

(defn pipe ^madnet.channel.Pipe []
  (let [pipe (java.nio.channels.Pipe/open)]
    (.configureBlocking (.sink pipe) false)
    (.configureBlocking (.source pipe) false)
    (Pipe. (PipeReader. (.source pipe)) (PipeWriter. (.sink pipe)))))
