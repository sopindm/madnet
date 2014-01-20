(ns madnet.sequence
  (:refer-clojure :exclude [take drop sequence take-last drop-last read range])
  (:import [madnet.sequence Range]))

;;
;; Range operation wrappers
;;

(defn size [range]
  (.size range))

(defn take! [n range]
  (.take range n))

(defn take-last! [n range]
  (.takeLast range n))

(defn drop! [n range]
  (.drop range n))

(defn drop-last! [n range]
  (.dropLast range n))

(defn expand! [n range]
  (.expand range n))

(defn split! [n range]
  (.split range n))

;;
;; Immutable range operations
;;

(defn take [n range]
  (take! n (.clone range)))

(defn take-last [n range]
  (take-last! n (.clone range)))

(defn drop [n range]
  (drop! n (.clone range)))

(defn drop-last [n range]
  (drop-last! n (.clone range)))

(defn expand [n range]
  (expand! n (.clone range)))

(defn split [n range]
  (let [clone (.clone range)]
    [(split! n clone) clone]))

(comment
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
    (+ (size seq) (free-space seq)))

  (defn free-space [seq]
    (.freeSpace seq))

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

  (defn circular-sequence [seq]
    (CircularSequence. seq))

  ;;
  ;; Additional sequence methods
  ;;

  (defn sequence
    ([^IBuffer buffer] (.sequence buffer 0 0 (.size buffer)))
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

  ;;
  ;; Reading/writing
  ;;

  (defn write [dest src]
    (let [pair (.write dest src)]
      [(.first pair) (.second pair)]))

  (defn read [dest src]
    (let [pair (.read dest src)]
      [(.first pair) (.second pair)])))
