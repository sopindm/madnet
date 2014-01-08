(ns madnet.slices-test
  (:refer-clojure :exclude [read > < take take-last split conj conj! write count get])
  (:require [khazad-dum.core :refer :all]
            [madnet.slices :refer :all])
  (:import [java.nio ByteBuffer CharBuffer
            BufferUnderflowException
            BufferOverflowException]
           [java.nio.charset Charset]))

(defmacro ?slice= [form [position size capacity]]
  `(do (?= (position ~form) ~position)
       (?= (count ~form) ~size)
       (?= (capacity ~form) ~capacity)))

(deftest making-slice
  (let [s1 (slice (ByteBuffer/allocate 1024))
        s2 (slice (ByteBuffer/allocate 1024) 128)
        s3 (slice (ByteBuffer/allocate 1024) 128 128)]
    (?slice= s1 [0 1024 1024])
    (?slice= s2 [0 128 1024])
    (?slice= s3 [128 128 1024])))

(deftest too-much-size-for-slice
  (?throws (slice (ByteBuffer/allocate 1024) 2024) IllegalArgumentException)
  (?throws (slice (ByteBuffer/allocate 1024) 512 1025) IllegalArgumentException)
  (?throws (slice (ByteBuffer/allocate 128) 129 1) IllegalArgumentException))

(deftest making-slice-with-modifyed-buffer
  (let [b (.limit (.position (ByteBuffer/allocate 1024) 100) 200)
        s (slice b)]
    (?slice= s [100 100 1024])))

(deftest moving-slices
  (let [s (slice (ByteBuffer/allocate 1024))
        s2 (> s 924)
        s3 (< s2 924)
        s4 (> s3 200)]
    (?slice= s2 [924 100 1024])
    (?slice= s3 [924 1024 1024])
    (?slice= s4 [100 824 1024])))

(deftest >-and-<-exceptions
  (let [s (slice (ByteBuffer/allocate 1024))]
    (?throws (< s 1) BufferOverflowException)
    (?throws (> (> s 600) 600) BufferUnderflowException)))

(deftest >!-and-<!
  (let [s (atom (slice (ByteBuffer/allocate 1024)))]
    (?slice= @s [0 1024 1024])
    (>! s 512)
    (?slice= @s [512 512 1024])
    (<! s 512)
    (?slice= @s [512 1024 1024])))

(deftest stake-and-stake-last
  (let [s (slice (ByteBuffer/allocate 1024))
        s2 (take s 124)
        s3 (take-last s 124)]
    (?slice= s [0 1024 1024])
    (?slice= s2 [0 124 1024])
    (?slice= s3 [900 124 1024])))

(deftest stake-and-stake-last-exceptions
  (let [s (slice (ByteBuffer/allocate 1024))]
    (?throws (take s 2000) BufferUnderflowException)
    (?throws (take-last s 2000) BufferUnderflowException)))

(deftest ssplit-and-sconj
  (let [s (slice (ByteBuffer/allocate 1024))
        [s2 s3] (split s 100)
        [s4 s5] (conj s3 50)]
    (?slice= s2 [0 100 1024])
    (?slice= s3 [100 924 1024])
    (?slice= s4 [0 50 1024])
    (?slice= s5 [100 974 1024])))

(deftest ssplit!-and-sconj!
  (let [s (atom (slice (ByteBuffer/allocate 1024)))
        s2 (split! s 900)
        s3 (conj! s 500)]
    (?slice= @s [900 624 1024])
    (?slice= s2 [0 900 1024])
    (?slice= s3 [0 500 1024])))

(defmacro ?buffer= [expr [pos limit capacity]]
  `(do (?= (.position ~expr) ~pos)
       (?= (.limit ~expr) ~limit)
       (?= (.capacity ~expr) ~capacity)))

(defmacro ?buffers= [expr [& specs]]
  `(do (?= (clojure.core/count ~expr) ~(clojure.core/count specs))
       ~@(map-indexed (fn [i spec] `(?buffer= (nth ~expr ~i) ~spec)) specs)))

(deftest slice-buffer
  (let [s (slice (ByteBuffer/allocate 1024))
        s2 (> s 768)
        s3 (> s2 256)
        s4 (< s3 256)]
    (?buffer= (buffer s) [0 1024 1024])
    (?buffer= (buffer s2) [768 1024 1024])
    (?buffer= (buffer s3) [0 0 1024])
    (?buffer= (buffer s4) [0 256 1024])))

(deftest slice-buffers
  (let [s (slice (ByteBuffer/allocate 1024))
        s2 (> s 768)
        s3 (> s2 256)
        s4 (< s2 256)]
    (?buffers= (buffers s) [[0 1024 1024]])
    (?buffers= (buffers s2) [[768 1024 1024]])
    (?buffers= (buffers s3) [])
    (?buffers= (buffers s4) [[768 1024 1024] [0 256 1024]])))

(deftest writing-and-reading-for-slices
  (let [s (slice (ByteBuffer/allocate 1024))]
    (?slice= (write s (byte-array (map byte (range -128 128))))
             [256 768 1024])
    (let [bytes (byte-array 256)]
      (?slice= (read s bytes) [256 768 1024])
      (?= (seq bytes) (range -128 128))))
  (let [s (slice (ByteBuffer/allocate 1024))
        s2 (< (> s 1000) 24)]
    (write s2 (byte-array (map byte (range 0 48))))
    (let [bytes (byte-array 48)]
      (read s2 bytes)
      (?= (seq bytes) (range 0 48)))))

(deftest read-and-write-with-offset-and-size-arguments
  (let [s (slice (ByteBuffer/allocate 1024))]
    (write s (byte-array (map byte (range 128))) 64)
    (let [bytes1 (byte-array 64)
          bytes2 (byte-array 128)]
      (read s bytes1)
      (read s bytes2 64)
      (?= (seq bytes1) (range 64))
      (?= (clojure.core/take 64 (seq bytes2)) (range 64))))
  (let [s (slice (ByteBuffer/allocate 1024))]
    (write s (byte-array (map byte (range 128))) 64 64)
    (let [bytes1 (byte-array 64)
          bytes2 (byte-array 128)
          bytes3 (byte-array 128)]
      (read s bytes1)
      (?= (seq bytes1) (seq (range 64 128)))
      (read s bytes2 64)
      (?= (seq (clojure.core/take 64 bytes2)) (seq (range 64 128)))
      (read s bytes3 64 64)
      (?= (seq (drop 64 bytes3)) (seq (range 64 128))))))

(deftest write!-and-read!
  (let [s (atom (slice (ByteBuffer/allocate 1024) 0))]
    (write! s (byte-array (map byte (range -128 128))))
    (?slice= @s [0 256 1024])
    (let [bytes (byte-array 256)]
      (read! s bytes)
      (?slice= @s [256 0 1024])
      (?= (seq bytes) (map byte (range -128 128))))))

(deftest write!-and-read!-with-size-argument
  (let [s (atom (slice (ByteBuffer/allocate 1024) 0))]
    (write! s (byte-array (map byte (range 128))) 64)
    (let [bytes1 (byte-array 64)
          bytes2 (byte-array 128)]
      (read @s bytes1)
      (?= (seq bytes1) (seq (range 64)))
      (read! s bytes2 64)
      (?= (seq (clojure.core/take 64 bytes2)) (seq (range 64))))))

(deftest write!-and-read!-with-offset-argument
  (let [s (atom (slice (ByteBuffer/allocate 1024) 0))]
    (write! s (byte-array (map byte (range 128))) 64 64)
    (let [bytes1 (byte-array 64)
          bytes2 (byte-array 128)]
      (read @s bytes1)
      (?= (seq bytes1) (seq (range 64 128)))
      (read! s bytes2 64 64)
      (?= (seq (drop 64 bytes2)) (seq (range 64 128))))))

(deftest writing-and-reading-for-byte-buffers
  (let [s1 (slice (ByteBuffer/allocate 1024) 128)
        s2 (slice (ByteBuffer/allocate 1024))
        s3 (slice (ByteBuffer/allocate 1024) 128)]
    (write s1 (byte-array (map byte (range 128))))
    (write s2 (buffer s1))
    (let [bytes (byte-array 128)]
      (read s2 bytes)
      (?= (seq bytes) (seq (range 128))))
    (read s2 (buffer s3))
    (let [bytes (byte-array 128)]
      (read s3 bytes)
      (?= (seq bytes) (seq (range 128))))))

(deftest writing-and-reading-slices-to-slices
  (let [s1 (slice (ByteBuffer/allocate 1024) 128)
        s2 (slice (ByteBuffer/allocate 1024) 1000 256)
        s3 (slice (ByteBuffer/allocate 1024) 128)]
    (write s1 (byte-array (map byte (range 128))))
    (write s2 s1 128)
    (read s1 s2 128 128)
    (write s3 s2 128 128)
    (let [bytes1 (byte-array 128)
          bytes2 (byte-array 256)
          bytes3 (byte-array 128)]
      (read s1 bytes1)
      (?= (seq bytes1) (seq (range 128)))
      (read s2 bytes2)
      (?= (seq bytes2) (seq (concat (range 128) (range 128))))
      (read s3 bytes3)
      (?= (seq bytes3) (seq (range 128))))))

(deftest slices-read-and-write-exceptions
  (let [s1 (slice (ByteBuffer/allocate 1024) 512)
        s2 (slice (ByteBuffer/allocate 1024) 768 512)]
    (?throws (write s1 (byte-array 1024)) BufferOverflowException)
    (?throws (read s1 (byte-array 1024)) BufferUnderflowException)
    (?throws (write s2 (byte-array 513)) BufferOverflowException)
    (?throws (read s2 (byte-array 513)) BufferUnderflowException)))

(deftest slices-seq-writing
  (let [s (slice (ByteBuffer/allocate 1024))]
    (write s (range -128 128))
    (let [bytes (byte-array 256)]
      (read s bytes)
      (?= (seq bytes) (seq (range -128 128))))))

(deftest slice-random-access
  (let [s (slice (ByteBuffer/allocate 256) 128 256)]
    (write s (range -128 128))
    (?= (seq (for [i (range 0 256)]
               (get s i)))
        (seq (range -128 128)))
    (?throws (get s -1) IndexOutOfBoundsException)
    (?throws (get s 256) IndexOutOfBoundsException)))

(deftest slice-to-seq
  (let [s (slice (ByteBuffer/allocate 256) 128 256)]
    (write s (range -128 128))
    (?= (seq s)
        (seq (range -128 128)))))

(deftest slices-for-char-buffers
  (let [s (slice (CharBuffer/allocate 256))
        string (clojure.core/take 256 (cycle "abc"))]
    (write s (char-array string))
    (?= (seq s) (seq string))
    (let [chars (char-array 256)]
      (read s chars)
      (?= (seq chars) (seq string)))
    (let [s2 (slice (CharBuffer/allocate 256))]
      (read s s2)
      (?= (seq s2) (seq string)))))

(deftest char-slices-to-bytes
  (let [bs (slice (ByteBuffer/allocate 256))
        cs (slice (CharBuffer/allocate 256))
        string (seq "some string")] 
    (write cs string)
    (write bs cs)
    (?= (seq (clojure.core/take (count string) bs))
        (seq (map byte string)))
    (let [moved-slice (< (> bs 255) 100)]
      (write moved-slice cs (count string))
      (?= (seq (clojure.core/take (count string) moved-slice))
          (seq (map byte string))))))

(comment 
  (deftest multibyte-chars-to-bytes
    (let [bs (slice (ByteBuffer/allocate 256))
          cs (slice (CharBuffer/allocate 256))
          string (seq "\u1100\u1101\u1102\u1103")
          bytes [-31 -124 -128 -31 -124 -127 -31 -124 -126 -31 -124 -125]]
      (write cs string)
      (write bs cs)
      (?= (seq (clojure.core/take 12 (seq bs))) bytes)
      (let [moved-slice (< (> bs 253) 253)]
        (write moved-slice cs)
        (?= (seq (clojure.core/take 12 (seq moved-slice)))
            bytes))
      (let [moved-slice (< (> bs 255) 255)]
        (write moved-slice cs)
        (?= (seq (clojure.core/take 12 (seq moved-slice)))
            bytes)))))
          
;reading/writing strings from/to bytes

;readonly and writeonly slices

;cycling and no-cycling slices
;separate slice-atoms, slice-refs and slice-agents packages??? or move to Java???


