(ns madnet.sequence
  (:refer-clojure :exclude [take drop sequence take-last drop-last])
  (:import [java.nio Buffer ByteBuffer CharBuffer]
           [java.nio.charset Charset]
           [madnet.sequence IBuffer ISequence ASequence]))

;;
;; ISequence and ASequence method wrappers
;;

(defn ^IBuffer buffer [^ISequence seq]
  (.buffer seq))

(defn size [^ISequence seq]
  (.size seq))

(defn position [^ASequence a-seq]
  (.position a-seq))

(defn capacity [^ISequence seq]
  (if-let [buffer (buffer seq)]
    (.size buffer)
    Long/MAX_VALUE))

(defn free-space [seq]
  (- (capacity seq) (size seq)))

(defn take [n ^ISequence seq]
  (when (neg? n) (throw (IllegalArgumentException.)))
  (when (> n (size seq)) (throw (java.nio.BufferUnderflowException.)))
  (.take seq n))

(defn drop [n ^ISequence seq]
  (when (neg? n) (throw (IllegalArgumentException.)))
  (when (> n (size seq)) (throw (java.nio.BufferUnderflowException.)))
  (.drop seq n))

(defn expand [n ^ISequence seq]
  (when (neg? n) (throw (IllegalArgumentException.)))
  (when (> n (free-space seq)) (throw (java.nio.BufferOverflowException.)))
  (.expand seq n))

;;
;; Additional sequence methods
;;

(defn sequence
  ([^IBuffer buffer] (.sequence buffer 0 0))
  ([buffer size] (expand size (sequence buffer)))
  ([buffer offset size] (drop offset (expand (+ offset size) (sequence buffer)))))

(defn take-last [n seq]
  (when (neg? n) (throw (IllegalArgumentException.)))
  (when (> n (size seq)) (throw (java.nio.BufferUnderflowException.)))
  (drop (- (size seq) n) seq))

(defn drop-last [n seq]
  (when (neg? n) (throw (IllegalArgumentException.)))
  (when (> n (size seq)) (throw (java.nio.BufferUnderflowException.)))
  (take (- (size seq) n) seq))

(defn split [n seq]
  [(take n seq) (drop n seq)])

(defn append [n seq]
  (let [expanded (expand n seq)]
    [(take-last n expanded) expanded]))

  