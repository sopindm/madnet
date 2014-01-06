(ns mindnet.slices-test
  (:refer-clojure :exclude [read > < take take-last split conj conj!])
  (:require [khazad-dum.core :refer :all]
            [mindnet.slices :refer :all])
  (:import [java.nio ByteBuffer BufferUnderflowException BufferOverflowException]))

(defmacro ?slice= [form [position size capacity]]
  `(do (?= (position ~form) ~position)
       (?= (size ~form) ~size)
       (?= (capacity ~form) ~capacity)))

(deftest making-slice
  (let [s (slice (ByteBuffer/allocate 1024))]
    (?slice= s [0 1024 1024])))

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
  `(do (?= (count ~expr) ~(count specs))
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

;write, read, write! and read! tests
;parrallel reading and writing
;try-write, try-read, try-write!, try-read!

;iseq for slice (for readonly use)

;slices for CharBuffer

;readonly and writeonly slices

;separate slice-atoms, slice-refs and slice-agents packages

