(ns madnet.sequence
  (:refer-clojure :exclude [take drop])
  (:require [madnet.buffer :as b])
  (:import [java.nio Buffer ByteBuffer CharBuffer]
           [java.nio.charset Charset]))

(defprotocol ISequence
  (buffer [this])
  (size [this])
  (take [this n])
  (drop [this n])
  (expand [this size]))

(deftype ASequence [buffer position size]
  ISequence
  (buffer [this] buffer)
  (size [this] size)
  (take [this n] (ASequence. buffer position n))
  (drop [this n] (ASequence. buffer (+ position n) (- size n)))
  (expand [this n] (ASequence. buffer position (+ size n))))

(defn position [seq]
  (.position seq))

(defn capacity [seq]
  (-> seq buffer b/size))

(defn free-space [seq]
  (- (capacity seq) (size seq)))

(defn take [n seq]
  (when (neg? n) (throw (IllegalArgumentException.)))
  (when (> n (size seq)) (throw (java.nio.BufferUnderflowException.)))
  (.take seq n))

(defn drop [n seq]
  (when (neg? n) (throw (IllegalArgumentException.)))
  (when (> n (size seq)) (throw (java.nio.BufferUnderflowException.)))
  (.drop seq n))

(defn sequence
  ([buffer] (.emptySequence buffer))
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

(defn expand [n seq]
  (when (neg? n) (throw (IllegalArgumentException.)))
  (when (> n (free-space seq)) (throw (java.nio.BufferOverflowException.)))
  (.expand seq n))



  