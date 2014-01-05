(ns mindnet.buffers
  (:import [java.nio ByteBuffer]))

;;
;; Buffers definition
;;

(def ^:dynamic *buffer-capacity* 1024)

(defn buffer []
  (let [b (ByteBuffer/allocate *buffer-capacity*)]
    {:writer b :reader (.limit (.duplicate b) 0)}))

(defn capacity [b]
  (.capacity (:writer b)))

(defn- before? [^ByteBuffer b1 ^ByteBuffer b2]
  (< (.position b1) (.position b2)))

(defn- size- [b]
  (- (.limit b) (.position b)))

(defn size [{:keys [reader writer]}]
  (if (before? reader writer)
    (size- reader)
    (+ (size- reader) (.position writer))))

(defn free-space [{:keys [reader writer]}]
  (if (before? writer reader)
    (size- writer)
    (+ (size- writer) (.position reader))))
;;
;; Generic reading/writing
;;

(defn- limit- [^ByteBuffer b1 ^ByteBuffer b2]
  (when (before? b1 b2)
    (.limit b1 (.position b2)))
  b1)

(defn- reset- [^ByteBuffer b1 ^ByteBuffer b2]
  (when (= (.position b1) (.capacity b1))
    (.position b1 0)
    (.limit b1 (.position b2)))
  b1)

(defn- sync- [b1 b2]
  (limit- b2 b1)
  (reset- b1 b2))

(defn write-byte! [{:keys [reader ^ByteBuffer writer] :as buffer} b]
  (.put writer ^byte b)
  (sync- writer reader)
  buffer)

(defn- write-bytes-! [{:keys [reader writer]} bytes offset size]
  (.put writer bytes offset size)
  (sync- writer reader))

(defn write-bytes! [{:keys [reader writer]:as buffer} bytes]
  (let [size1 (min (count bytes) (size- writer))
        size2 (- (count bytes) size1)]
    (write-bytes-! buffer bytes 0 size1)
    (when (pos? size2) (write-bytes-! buffer bytes size1 size2)))
  buffer)

(defn peek-byte [{^ByteBuffer reader :reader}]
  (.mark reader)
  (let [val (.get reader)]
    (.reset reader)
    val))

(defn read-byte! [{:keys [^ByteBuffer reader writer]}]
  (let [value (.get reader)]
    (sync- reader writer)
    value))

(defn- read-bytes-! [{:keys [reader writer]} buffer offset size]
  (.get reader buffer offset size)
  (sync- reader writer))

(defn read-bytes! [{:keys [reader writer] :as buffer} size]
  (let [value (byte-array size)
        size1 (min (size- reader) size)
        size2 (- size size1)]
    (read-bytes-! buffer value 0 size1)
    (when (pos? size2) (read-bytes-! buffer value size1 size2))
    value))


        