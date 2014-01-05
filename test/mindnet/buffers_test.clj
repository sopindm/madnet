(ns mindnet.buffers-test
  (:require [khazad-dum.core :refer :all]
            [mindnet.buffers :refer :all]))

(deftest writing-and-reading-bytes
  (let [b (buffer)]
    (write-byte! b (byte 123))
    (dotimes [_ 10]
      (?= (peek-byte b) (byte 123)))
    (?= (read-byte! b) (byte 123))))

(deftest reading-from-empty-buffer
  (let [b (buffer)]
    (?throws (peek-byte b) java.nio.BufferUnderflowException)
    (?throws (read-byte! b) java.nio.BufferUnderflowException)))

(deftest overflowing-buffer
  (let [b (buffer)]
    (dotimes [_ *buffer-capacity*]
      (write-byte! b (byte 123))))
  (let [b (buffer)]
    (?throws (dotimes [_ (inc *buffer-capacity*)]
               (write-byte! b (byte 123)))
             java.nio.BufferOverflowException)))

(deftest write-read-sequence
  (let [b (buffer)]
    (dotimes [i (* *buffer-capacity* 2)]
      (write-byte! b (byte 123))
      (when (zero? (mod i 2))
        (?= (read-byte! b) 123)))
    (?throws (write-byte! b (byte 123)) java.nio.BufferOverflowException)
    (dotimes [i *buffer-capacity*]
      (?= (read-byte! b) 123))))

;writing and reading different java types
;writing and reading byte arrays
;writing and reading strings
;writing and reading clojure objects (+generic)

;concatenating buffers
;getting subbuffers
;seq + string wrapper

;benchmarks???

;buffer options (direct?, size)

;persistent buffers (channels)

;java interface to buffer???