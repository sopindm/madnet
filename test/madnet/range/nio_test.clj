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

(deftest nio-range-operations
  (let [r (nrange 15 32 (ByteBuffer/allocate 100))]
    (r/expand! 10 r)
    (?buffer= (.buffer r) 15 42 100)
    (r/drop! 12 r)
    (?buffer= (.buffer r) 27 42 100)
    (?throws (r/expand! 59 r) IllegalArgumentException)
    (?throws (r/drop! 16 r) IndexOutOfBoundsException)))

;nio range circular operations

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
    (?range= cr [0 10])
    (.put b 0 (byte 123))
    (?= (.get cr 0) (byte 123))
    (r/drop! 5 r)
    (?range= cr [0 10])))

(deftest byte-range-writing-and-reading
  (letfn [(fill-buffer [n] 
            (let [b (ByteBuffer/allocate n)]
              (dotimes [i n] (.put b i (+ 1 (* i i))))
              b))
          (operation-check [f]
            (let [b (fill-buffer 10)
                  r1 (byte-range 0 10 b)
                  r2 (byte-range 0 10 (ByteBuffer/allocate 10))
                  rc (.clone r2)]
              (f r2 r1)
              (?range= r1 [10 10])
              (?range= r2 [10 10])
              (?= (seq rc) [1 2 5 10 17 26 37 50 65 82])))]
    (operation-check r/write!)
    (operation-check #(r/read! %2 %1))))

(deftest writing-more-and-less-to-byte-buffer
  (let [b (ByteBuffer/allocate 10)
        source (byte-range 0 10 b)
        dest (byte-range 0 5 (ByteBuffer/allocate 10))
        dest-clone (.clone dest)]
    (dotimes [i 10]
      (.put b i (+ i 3)))
    (r/write! dest source)
    (?range= source [5 10])
    (?range= dest [5 5])
    (?= (seq dest-clone) [3 4 5 6 7]))
  (let [b (ByteBuffer/allocate 10)
        source (byte-range 0 5 b)
        dest (byte-range 0 10 (ByteBuffer/allocate 10))
        dest-clone (.clone dest)]
    (dotimes [i 5]
      (.put b i (+ i 2)))
    (r/write! dest source)
    (?range= source [5 5])
    (?range= dest [5 10])
    (?= (seq (r/take 5 dest-clone)) [2 3 4 5 6])))

;circular writing to byte buffer

; circular nio range
;;making
;;operations
;;ranges
;;read/write

;char range
;;random access
;;iterable
;;converting to/from bytes
;;;conversion methods
;;;convertors range

;ranges for byte[] and char[]




