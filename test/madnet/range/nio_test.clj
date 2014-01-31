(ns madnet.range.nio-test
  (:require [khazad-dum.core :refer :all]
            [madnet.range-test :refer :all]
            [madnet.range :as r]
            [madnet.range.nio :as n])
  (:import [java.nio ByteBuffer]
           [madnet.range]
           [madnet.range.nio Range ByteRange]))

(defmacro ?buffer= [expr position limit capacity]
  `(do (?= (.position ~expr) ~position)
       (?= (.limit ~expr) ~limit)
       (?= (.capacity ~expr) ~capacity)))

;;
;; nio range
;;

(defn- nrange [begin end buffer]
  (Range. begin end buffer))

(deftest making-nio-range
  (let [b (ByteBuffer/allocate 1024)
        r (nrange 128 512 b)]
    (?range= r [128 512])
    (?buffer= (.buffer r) 128 512 1024)))

(deftest cloning-nio-range
  (let [b (ByteBuffer/allocate 1024)
        r (nrange 64 256 b)]
    (?range= (.clone r) [64 256])
    (?true (identical? (.buffer r) (.buffer (.clone r))))))

(deftest nio-range-operations
  (let [r (nrange 15 32 (ByteBuffer/allocate 100))]
    (r/expand! 10 r)
    (?buffer= (.buffer r) 15 42 100)
    (r/drop! 12 r)
    (?buffer= (.buffer r) 27 42 100)
    (?throws (r/expand! 59 r) IllegalArgumentException)
    (?throws (r/drop! 16 r) IllegalArgumentException)))

;;
;; byte range
;;

(defn byte-range [begin end buffer]
  (ByteRange. begin end buffer))

(deftest making-byte-range
  (let [r (byte-range 128 512 (ByteBuffer/allocate 1024))]
    (?range= r [128 512])))

(deftest byte-range-random-access
  (let [b (ByteBuffer/allocate 1024)
        r (byte-range 128 512 b)]
    (.put b 132 (byte 123))
    (?= (.get r 4) (byte 123))))

(deftest byte-range-as-seq
  (let [b (ByteBuffer/allocate 100)
        r (byte-range 2 10 b)]
    (dotimes [i 10]
      (.put b i (byte i)))
    (?= (seq r) [2 3 4 5 6 7 8 9])))

(deftest byte-range-cloning
  (let [b (ByteBuffer/allocate 100)
        r (byte-range 0 10 b)
        cr (.clone r)]
    (.put b 0 (byte 123))
    (?= (.get cr 0) (byte 123))))

;;reading/writing

; circular nio range
;;making
;;ranges
;;read/write

;char range
;;random access
;;iterable
;;converting to/from bytes

;ranges for byte[] and char[]




