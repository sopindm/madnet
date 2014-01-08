(ns madnet.slices
  (:refer-clojure :exclude [read write < > take take-last split conj conj! count get])
  (:import [java.nio Buffer ByteBuffer CharBuffer]
           [java.nio.charset Charset]))

(defmulti count (comp #(if (.isArray ^Class %) :array %) class))

(defmethod count :array [buffer]
  (clojure.core/count buffer))

(defmethod count clojure.lang.Counted [buffer]
  (clojure.core/count buffer))

(defmethod count clojure.lang.ISeq [buffer]
  (clojure.core/count buffer))

(prefer-method count clojure.lang.Counted clojure.lang.ISeq)

;;
;; nio buffers
;;

(defn- buffer+ [buffer dlt]
  (.position (.duplicate buffer) (+ (.position buffer) dlt)))

(defn- ^Buffer limit [^Buffer buffer size]
  (.limit buffer (min (.limit buffer) (+ (.position buffer) size))))

(defmethod count Buffer [^Buffer buffer]
  (- (.limit buffer) (.position buffer)))

;;
;; Slice type
;;

(declare read)

(deftype Slice [buffer position size]
  clojure.lang.Seqable
  (seq [this]
    (let [array ((cond (isa? (class buffer) ByteBuffer) byte-array
                       (isa? (class buffer) CharBuffer) char-array) (count this))]
      (read this array)
      (seq array))))

(defn slice 
  ([buffer] (slice buffer (count buffer)))
  ([^ByteBuffer buffer size] (slice buffer (.position buffer) size))
  ([buffer position size]
     (when (clojure.core/> size (.capacity buffer))
       (throw (IllegalArgumentException.)))
     (when (clojure.core/> position (.capacity buffer))
       (throw (IllegalArgumentException.)))
     (Slice. buffer position size)))

(defn position [slice]
  (.position slice))

(defmethod count Slice [slice]
  (.size slice))

(defn capacity [slice]
  (.capacity ^ByteBuffer (.buffer slice)))

;;
;; Slice operating
;;

(defn speek [slice dlt]
  slice)

(defn > [slice dlt]
  (let [capacity (capacity slice)
        position (+ (position slice) dlt)
        size (- (count slice) dlt)]
    (when (neg? size) (throw (java.nio.BufferUnderflowException.)))
    (Slice. (.buffer slice) (mod position capacity) size)))

(defn >! [slice-atom dlt]
  (swap! slice-atom #(> % dlt)))

(defn < [slice dlt]
  (let [size (+ (count slice) dlt)]
    (when (clojure.core/> size (capacity slice)) (throw (java.nio.BufferOverflowException.)))
    (Slice. (.buffer slice) (position slice) size)))

(defn <! [slice-atom dlt]
  (swap! slice-atom #(< % dlt)))

(defn take [s size]
  (when (clojure.core/> size (count s)) (throw (java.nio.BufferUnderflowException.)))
  (slice (.buffer s) (position s) size))

(defn take-last [s size]
  (when (clojure.core/> size (count s)) (throw (java.nio.BufferUnderflowException.)))
  (> s (- (count s) size)))

(defn split [slice size]
  (let [first (take slice size)
        rest (> slice size)]
    [first rest]))

(defn conj [slice size]
  (let [conj (< slice size)]
    [(take-last conj size) conj]))
  
(defn split! [slice-atom size]
  (let [[split new] (split @slice-atom size)]
    (reset! slice-atom new)
    split))

(defn conj! [slice-atom size]
  (let [[part all] (conj @slice-atom size)]
    (reset! slice-atom all)
    part))

(defn buffer [slice]
  (let [buffer (.buffer slice)
        position (position slice)]
    (-> buffer
        .duplicate
        (.position position)
        (.limit (min (capacity slice) (+ position (count slice)))))))

(defn buffers [slice]
  (if (pos? (count slice))
    (let [buffer (buffer slice)]
      (cons buffer (buffers (> slice (count buffer)))))))

(defn get [slice i]
  (when (or (clojure.core/< i 0) (>= i (count slice))) (throw (IndexOutOfBoundsException.))) 
  (.get (buffer slice) (mod (+ (position slice) i) (capacity slice))))

;;
;; Copy multimethod
;;

(defmulti copy (fn [src dst src-offset dst-offset size & _] [(class src) (class dst)]))

(defmethod copy [(Class/forName "[B") ByteBuffer] [src dst src-offset dst-offset size]
  (.put ^ByteBuffer (buffer+ dst dst-offset) src src-offset size)
  size)

(defmethod copy [ByteBuffer (Class/forName "[B")] [src dst src-offset dst-offset size]
  (.get ^ByteBuffer (buffer+ src src-offset) dst dst-offset size)
  size)

(defmethod copy [ByteBuffer ByteBuffer] [src dst src-offset dst-offset size]
  (.put ^ByteBuffer (limit (buffer+ dst dst-offset) size)
        ^ByteBuffer (limit (buffer+ src src-offset) size))
  size)

(defmethod copy [(Class/forName "[C") CharBuffer] [src dst src-offset dst-offset size]
  (.put ^CharBuffer (buffer+ dst dst-offset) src src-offset size)
  size)

(defmethod copy [CharBuffer (Class/forName "[C")] [src dst src-offset dst-offset size]
  (.get ^CharBuffer (buffer+ src src-offset) dst dst-offset size)
  size)

(defmethod copy [CharBuffer CharBuffer] [src dst src-offset dst-offset size]
  (.put (limit (buffer+ dst dst-offset) size)
        (limit (buffer+ src src-offset) size))
  size)

(defmethod copy [CharBuffer ByteBuffer] [src dst src-offset dst-offset size & [charset]]
  (let [charset (Charset/forName (or charset "UTF-8"))
        encoder (.newEncoder charset)]
    (.encode encoder (buffer+ src src-offset) (buffer+ dst dst-offset) true)
    size))

(defmethod copy [Slice Object] [src dst src-offset dst-offset size]
  (- (reduce (fn [offset buffer]
               (let [size (min (- (+ size dst-offset) offset) (count buffer))]
                 (copy buffer dst 0 offset size)
                 (+ offset size)))
             dst-offset (buffers (take (> src src-offset) size)))
     dst-offset))

(defmethod copy [Object Slice] [src dst src-offset dst-offset size]
  (let [buffers (buffers (> dst dst-offset))]
    (when (clojure.core/< (reduce + (map count buffers)) size)
      (throw (java.nio.BufferOverflowException.)))
    (- (reduce (fn [offset buffer]
                 (let [size (min (- (+ size src-offset) offset) (count buffer))]
                   (copy src buffer offset 0 size)
                   (+ offset size)))
               src-offset buffers)
       src-offset)))

(prefer-method copy [Object Slice] [Slice Object])

(defmethod copy [clojure.lang.ISeq ByteBuffer] [src dst src-offset dst-offset size]
  (copy (byte-array (map byte src)) dst src-offset dst-offset size))

(defmethod copy [clojure.lang.ISeq CharBuffer] [src dst src-offset dst-offset size]
  (copy (char-array src) dst src-offset dst-offset size))

;;
;; Read/write
;;

(defn write
  ([slice src] (write slice src (count src)))
  ([slice src size] (write slice src 0 size))
  ([slice src offset size] (> slice (copy src slice offset 0 size))))

(defn read
  ([slice dest] (read slice dest (count dest)))
  ([slice dest size] (read slice dest 0 size))
  ([slice dest offset size] (> slice (copy slice dest 0 offset size))))

(defn write!
  ([slice-atom bytes] (write! slice-atom bytes (count bytes)))
  ([slice-atom bytes size] (write! slice-atom bytes 0 size))
  ([slice-atom bytes offset size] (write (conj! slice-atom size) bytes offset size)
     slice-atom))

(defn read!
  ([slice-atom dest] (read! slice-atom dest (count dest)))
  ([slice-atom bytes size] (read! slice-atom bytes 0 size))
  ([slice-atom bytes offset size] (read (split! slice-atom size) bytes offset size)
     slice-atom))
