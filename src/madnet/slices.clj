(ns madnet.slices
  (:refer-clojure :exclude [read write < > take take-last split conj conj! count])
  (:import [java.nio ByteBuffer]))

(defmulti count class)

;;
;; NIO Buffers
;;

(defn- buffer+ [buffer dlt]
  (.position (.duplicate buffer) (+ (.position buffer) dlt)))

(defn- limit [buffer size]
  (.limit buffer (min (.limit buffer) (+ (.position buffer) size))))

(defmethod count ByteBuffer [buffer]
  (- (.limit buffer) (.position buffer)))

;;
;; Slice type
;;

(defrecord Slice [buffer position size])

(defn slice 
  ([buffer] (slice buffer (count buffer)))
  ([^ByteBuffer buffer size] (slice buffer (.position buffer) size))
  ([buffer position size] (Slice. buffer position size)))

(defn position [slice]
  (:position slice))

(defmethod count Slice [slice]
  (:size slice))

(defn capacity [slice]
  (.capacity ^ByteBuffer (:buffer slice)))

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
    (assoc slice
      :position (mod position capacity)
      :size size)))

(defn >! [slice-atom dlt]
  (swap! slice-atom #(> % dlt)))

(defn < [slice dlt]
  (let [size (+ (count slice) dlt)]
    (when (clojure.core/> size (capacity slice)) (throw (java.nio.BufferOverflowException.)))
    (assoc slice :size size)))

(defn <! [slice-atom dlt]
  (swap! slice-atom #(< % dlt)))

(defn take [s size]
  (when (clojure.core/> size (:size s)) (throw (java.nio.BufferUnderflowException.)))
  (slice (:buffer s) (:position s) size))

(defn take-last [s size]
  (when (clojure.core/> size (:size s)) (throw (java.nio.BufferUnderflowException.)))
  (> s (- (count s) size)))

(defn split [slice size]
  (let [first (take slice size)
        rest (> slice size)]
    [first rest]))

(defn conj [slice size]
  (let [conj (< slice size)]
    [(take-last conj size) conj]))
  
(defn split! [slice-atom size]
  (:split (swap! slice-atom 
                 #(let [[split new] (split % size)]
                    (assoc new :split split)))))

(defn conj! [slice-atom size]
  (:conj (swap! slice-atom
                #(let [[part all] (conj % size)]
                   (assoc all
                     :conj part)))))

(defn buffer [slice]
  (let [{:keys [^ByteBuffer buffer position size]} slice
        capacity (capacity slice)]
    (-> buffer
        .duplicate
        (.position position)
        (.limit (min capacity (+ position size))))))

(defn buffers [slice]
  (if (pos? (count slice))
    (let [buffer (buffer slice)]
      (cons buffer (buffers (> slice (count buffer)))))))

;;
;; Copy multimethod
;;

(defmulti copy (fn [src dst src-offset dst-offset size] [(class src) (class dst)]))

(defmethod copy [(Class/forName "[B") ByteBuffer] [src dst src-offset dst-offset size]
  (.put (buffer+ dst dst-offset) src src-offset size))

(defmethod copy [ByteBuffer (Class/forName "[B")] [src dst src-offset dst-offset size]
  (.get (buffer+ src src-offset) dst dst-offset size))

(defmethod copy [ByteBuffer ByteBuffer] [src dst src-offset dst-offset size]
  (.put (limit (buffer+ dst dst-offset) size)
        (limit (buffer+ src src-offset) size)))

(defmethod copy [Slice Object] [src dst src-offset dst-offset size]
  (reduce (fn [offset buffer]
            (let [size (min (- (+ size dst-offset) offset) (count buffer))]
              (copy buffer dst 0 offset size)
              (+ offset size)))
          dst-offset (buffers (take (> src src-offset) size)))
  src)

(defmethod copy [Object Slice] [src dst src-offset dst-offset size]
  (reduce (fn [offset buffer]
            (let [size (min (- (+ size src-offset) offset) (count buffer))]
              (copy src buffer offset 0 size)
              (+ offset size)))
          src-offset (buffers (take (> dst dst-offset) size)))
  src)

(prefer-method copy [Object Slice] [Slice Object])

;;
;; Read/write
;;

(defmethod count (Class/forName "[B") [buffer]
  (clojure.core/count buffer))

(defn write
  ([slice src] (write slice src (count src)))
  ([slice src size] (write slice src 0 size))
  ([slice src offset size] (copy src slice offset 0 size)))

(defn read
  ([slice dest] (read slice dest (count dest)))
  ([slice dest size] (read slice dest 0 size))
  ([slice dest offset size] (copy slice dest 0 offset size)))

(defn write!
  ([slice-atom bytes] (write! slice-atom bytes (count bytes)))
  ([slice-atom bytes size] (write! slice-atom bytes 0 size))
  ([slice-atom bytes offset size] (write (conj! slice-atom size) bytes offset size)
     slice-atom))

(defn read!
  ([slice-atom bytes] (read! slice-atom bytes (count bytes)))
  ([slice-atom bytes size] (read! slice-atom bytes 0 size))
  ([slice-atom bytes offset size]
     (read (split! slice-atom size) bytes offset size)
     slice-atom))
