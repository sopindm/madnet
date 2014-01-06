(ns mindnet.buffers
  (:require [mindnet.slices :as s])
  (:import [java.nio ByteBuffer]))

;;
;; Buffers definition
;;

(def ^:dynamic *buffer-capacity* 1024)

(defn buffer []
  (let [b (ByteBuffer/allocate *buffer-capacity*)]
    {:writer (atom (s/slice b)) :reader (atom (s/slice b 0))}))

(defn capacity [b]
  (-> b :writer deref s/capacity))

(defn size [b]
  (-> b :reader deref s/size))

(defn free-space [b]
  (-> b :writer deref s/size))

;;
;; Generic reading/writing
;;

(defn write-byte! [{:keys [reader writer] :as buffer} b]
  (.put ^ByteBuffer (s/buffer @writer) ^byte b)
  (s/slice>! writer 1)
  (s/slice<! reader 1)
  buffer)

(defn write-bytes! [{:keys [reader writer]:as buffer} bytes]
  (s/write @writer bytes)
  (s/slice>! writer (count bytes))
  (s/slice<! reader (count bytes))
  buffer)

(defn peek-byte [{reader :reader}]
  (.get ^ByteBuffer (s/buffer @reader)))

(defn read-byte! [{:keys [reader writer]}]
  (let [value (.get ^ByteBuffer (s/buffer @reader))]
    (s/slice>! reader 1)
    (s/slice<! writer 1)
    value))

(defn read-bytes! [{:keys [reader writer] :as buffer} size]
  (let [value (byte-array size)]
    (s/read @reader value)
    (s/slice>! reader size) 
    (s/slice<! writer size)
    value))

        