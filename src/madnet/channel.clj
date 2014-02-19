(ns madnet.channel
  (:refer-clojure :exclude [read])
  (:import [madnet.channel IChannel Result]))

;;
;; Reading/writing
;;

(defn readable? [ch]
  (.readable ch))

(defn writeable? [ch]
  (.writeable ch))

(defn write! [dest src]
  (or (.write dest src) (.read src dest) (throw (UnsupportedOperationException.))))

(defn read! [dest src]
  (or (.read dest src) (.write src dest) (throw (UnsupportedOperationException.))))

(defn write [dest src]
  (let [writen (.clone dest)
        read (.clone src)]
    (write! writen read)
    [writen read]))

(defn read [dest src]
  (let [read (.clone dest)
        writen (.clone src)]
    (read! read writen)
    [read writen]))

;;
;; Pipes
;;

(deftype Pipe [pipe]
  IChannel
  (clone [this] this)
  (readable [this] (-> pipe .source .isOpen))
  (closeRead [this] (-> pipe .source .close))
  (writeable [this] (-> pipe .sink .isOpen))
  (closeWrite [this] (-> pipe .sink .close))
  (write [this channel]
    (when (instance? madnet.range.nio.ByteRange channel)
      (let [buffer (.buffer channel)
            begin (.position buffer)]
        (.write (.sink pipe) (.buffer channel))
        (Result. (- (.position buffer) begin)
                 (- (.position buffer) begin)))))
  (read [this channel]
    (when (instance? madnet.range.nio.ByteRange channel)
      (let [buffer (.buffer channel)
            begin (.position buffer)]
        (.read (.source pipe) (.buffer channel))
        (Result. (- (.position buffer) begin)
                 (- (.position buffer) begin))))))

(defn pipe []
  (let [pipe (java.nio.channels.Pipe/open)]
    (.configureBlocking (.sink pipe) false)
    (.configureBlocking (.source pipe) false)
    (Pipe. pipe)))

(defn close! [channel & options]
  (when (or (some #{:write} options) (empty? options))
    (.closeWrite channel))
  (when (or (some #{:read} options) (empty? options))
    (.closeRead channel))
  channel)
