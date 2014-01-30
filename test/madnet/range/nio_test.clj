(ns madnet.range.nio-test
  (:require [khazad-dum.core :refer :all]
            [madnet.range-test :refer :all]
            [madnet.range :as r]
            [madnet.range.nio :as n])
  (:import [java.nio ByteBuffer]
           [madnet.range]
           [madnet.range.nio]))

(defmacro ?buffer= [expr position limit capacity]
  `(do (?= (.position ~expr) ~position)
       (?= (.limit ~expr) ~limit)
       (?= (.capacity ~expr) ~capacity)))

;;
;; nio range
;;

(deftest making-nio-range
  (let [b (ByteBuffer/allocate 1024)
        r (n/range 128 512 b)]
    (?range= r [128 512])
    (?buffer= (.buffer r) 128 512 1024)))

(deftest cloning-nio-range
  (let [b (ByteBuffer/allocate 1024)
        r (n/range 64 256 b)]
    (?range= (.clone r) [64 256])
    (?true (identical? (.buffer r) (.buffer (.clone r))))))

(deftest nio-range-operations
  (let [r (n/range 15 32 (ByteBuffer/allocate 100))]
    (r/expand! 10 r)
    (?buffer= (.buffer r) 15 42 100)
    (r/drop! 12 r)
    (?buffer= (.buffer r) 27 42 100)
    (?throws (r/expand! 59 r) IllegalArgumentException)
    (?throws (r/drop! 16 r) IllegalArgumentException)))

;;
;; byte range
;;

(deftest making-byte-range
  (let [r (n/byte-range 128 512 (ByteBuffer/allocate 1024))]
    (?range= r [128 512])))

;;cloning
;;random access
;;iterable
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




