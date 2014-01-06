(ns mindnet.buffers-test
  (:require [khazad-dum.core :refer :all]
            [mindnet.buffers :refer :all])
  (:import [java.nio BufferUnderflowException BufferOverflowException]))

(deftest writing-and-reading-bytes
  (let [b (buffer)]
    (write-byte! b (byte 123))
    (dotimes [_ 10]
      (?= (peek-byte b) (byte 123)))
    (?= (read-byte! b) (byte 123))))

(deftest reading-from-empty-buffer
  (let [b (buffer)]
    (?throws (peek-byte b) BufferUnderflowException)
    (?throws (read-byte! b) BufferUnderflowException)))

(deftest overflowing-buffer
  (let [b (buffer)]
    (dotimes [_ *buffer-capacity*]
      (write-byte! b (byte 123))))
  (let [b (buffer)]
    (?throws (dotimes [_ (inc *buffer-capacity*)]
               (write-byte! b (byte 123)))
             BufferOverflowException)))

(deftest write-read-sequence
  (let [b (buffer)]
    (dotimes [i (* *buffer-capacity* 2)]
      (write-byte! b (byte 123))
      (when (zero? (mod i 2))
        (?= (read-byte! b) 123)))
    (?throws (write-byte! b (byte 123)) BufferOverflowException)
    (dotimes [i *buffer-capacity*]
      (?= (read-byte! b) 123))
    (?throws (read-byte! b) BufferUnderflowException)))

(deftest buffer-size-capacity-and-free-space
  (let [b (buffer)]
    (?= (capacity b) *buffer-capacity*)
    (?= (size b) 0)
    (?= (free-space b) *buffer-capacity*)
    (dotimes [i *buffer-capacity*]
      (write-byte! b (byte 123)))
    (?= (capacity b) *buffer-capacity*)
    (?= (size b) *buffer-capacity*)
    (?= (free-space b) 0)
    (dotimes [i (quot *buffer-capacity* 2)]
      (read-byte! b))
    (?= (capacity b) *buffer-capacity*)
    (?= (size b) (- *buffer-capacity* (quot *buffer-capacity* 2)))
    (?= (free-space b) (quot *buffer-capacity* 2))))

(deftest buffer-ring-size-and-capacity
  (let [b (buffer)]
    (dotimes [i (dec *buffer-capacity*)]
      (write-byte! b (byte 123)))
    (dotimes [i (quot *buffer-capacity* 2)]
      (read-byte! b))
    (?= (free-space b) (inc (quot *buffer-capacity* 2)))
    (dotimes [i (quot *buffer-capacity* 2)]
      (write-byte! b (byte 123)))
    (?= (size b) (dec *buffer-capacity*))))

(deftest writing-and-reading-byte-arrays
  (let [b (buffer)
        byte-seq (fn [size] (map byte (take size (cycle (range -128 128)))))]
    (write-bytes! b (byte-array (byte-seq *buffer-capacity*)))
    (?= (seq (read-bytes! b *buffer-capacity*))
        (take *buffer-capacity* (cycle (range -128 128))))
    (?= (free-space b) (capacity b))
    (write-bytes! b (byte-array (byte-seq (quot *buffer-capacity* 2))))
    (?= (seq (read-bytes! b (quot *buffer-capacity* 2)))
        (byte-seq (quot *buffer-capacity* 2)))
    (write-bytes! b (byte-array (byte-seq *buffer-capacity*)))
    (?= (seq (read-bytes! b *buffer-capacity*)) (byte-seq *buffer-capacity*))))

;switch from *buffer-capacity* to (capacity b)
;don't support peek-byte and read/write-byte

;read bytes in existing array
;writing bytes from byte seq's
;writing/reading from/to other buffer

;writing and reading too much

;reading and writing slices (chunks)

;conditional writes and read

;writing and reading byte arrays
;writing and reading strings (+encoding, default encoding)

;splitting and concatenating
;seq + string wrapper

;benchmarks???

;buffer options (direct?, size)

;persistent buffers (channels)

;java interface to buffer???