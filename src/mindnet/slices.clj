(ns mindnet.slices
  (:refer-clojure :exclude [read < > take take-last split conj conj!])
  (:import [java.nio ByteBuffer]))

;;
;; Slice type
;;

(defn- buffer-size [^ByteBuffer buffer]
  (- (.limit buffer) (.position buffer)))

(defn slice 
  ([buffer] (slice buffer (buffer-size buffer)))
  ([^ByteBuffer buffer size] (slice buffer (.position buffer) size))
  ([buffer position size] {:buffer buffer :position position :size size}))

(defn position [slice]
  (:position slice))

(defn size [slice]
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
        size (- (size slice) dlt)]
    (when (neg? size) (throw (java.nio.BufferUnderflowException.)))
    (assoc slice
      :position (mod position capacity)
      :size size)))

(defn >! [slice-atom dlt]
  (swap! slice-atom #(> % dlt)))

(defn < [slice dlt]
  (let [size (+ (size slice) dlt)]
    (when (clojure.core/> size (capacity slice)) (throw (java.nio.BufferOverflowException.)))
    (assoc slice :size size)))

(defn <! [slice-atom dlt]
  (swap! slice-atom #(< % dlt)))

(defn take [s size]
  (when (clojure.core/> size (:size s)) (throw (java.nio.BufferUnderflowException.)))
  (slice (:buffer s) (:position s) size))

(defn take-last [s size]
  (when (clojure.core/> size (:size s)) (throw (java.nio.BufferUnderflowException.)))
  (> s (- (:size s) size)))

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
  (let [{size :size} slice]
    (if (pos? size)
      (let [buffer (buffer slice)]
        (cons buffer
              (buffers (> slice (buffer-size buffer))))))))

(defn write [slice bytes]
  (reduce (fn [offset ^ByteBuffer buffer]
            (let [size (min (- (count bytes) offset) (buffer-size buffer))]
              (.put buffer bytes offset size)
              (+ offset size)))
          0 (buffers slice)))

(defn read [slice bytes]
  (reduce (fn [offset ^ByteBuffer buffer]
            (let [size (min (- (count bytes) offset) (buffer-size buffer))]
              (.get buffer bytes offset size)
              (+ offset size)))
          0 (buffers slice)))
