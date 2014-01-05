(ns mindnet.buffers
  (:import [java.nio ByteBuffer]))

(def ^:dynamic *buffer-capacity* 1024)

(defn buffer []
  (let [b (ByteBuffer/allocate *buffer-capacity*)]
    {:writer b :reader (.limit (.duplicate b) 0)}))

(defn write-byte! [{:keys [reader writer] :as buffer} b]
  (when (= (.position writer) (.capacity writer))
    (.position writer 0)
    (.limit writer (.position reader)))
  (.put writer b)
  (if (and (<= (.position writer) (.position reader))
           (= (.limit writer) (.position reader)))
    (.limit reader (.capacity writer))
    (.limit reader (.position writer)))
  buffer)

(defn peek-byte [{reader :reader}]
  (.mark reader)
  (let [val (.get reader)]
    (.reset reader)
    val))

(defn read-byte! [{:keys [reader writer]}]
  (let [value (.get reader)]
    (when (< (.position writer) (.position reader))
      (.limit writer (.position reader)))
    (when (= (.position reader) (.capacity reader))
      (.position reader 0)
      (.limit reader (.position writer)))
    value))