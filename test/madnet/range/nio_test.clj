(ns madnet.range.nio-test
  (:require [khazad-dum.core :refer :all]
            [madnet.range-test :refer :all]
            [madnet.range :as r]
            [madnet.range.nio :as n])
  (:import [java.nio ByteBuffer CharBuffer]
           [java.nio.charset Charset]
           [madnet.range]
           [madnet.range.nio Range ByteRange CharRange]))

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

(defn- char-range [begin end buffer]
  (CharRange. begin end buffer))

(deftest making-char-range
  (let [b (CharBuffer/allocate 10)
        r (char-range 2 7 b)]
    (?range= r [2 7])
    (?range= (.clone r) [2 7])
    (?true (isa? (type (.clone r)) CharRange))))

(deftest char-range-random-access
  (let [b (CharBuffer/allocate 10)
        r (char-range 2 7 b)]
    (.put b 4 \h)
    (?= (.get r 2) \h)))

(deftest char-range-as-seq
  (let [b (CharBuffer/wrap "Hello, world!!!")
        r (char-range 7 12 b)]
    (?= (seq r) (seq "world"))))

;char range writing/reading

(defn- byte-buffer [seq]
  )

(defn- byte-range- [seq begin end]
  (let [bb (byte-buffer seq)]
    (byte-range begin end bb)))

(deftest bytes-to-chars-conversion
  (letfn [(byte-buffer [seq]
            (let [bb (ByteBuffer/allocate (count seq))]
              (dotimes [i (count seq)]
                (.put bb i (byte (nth seq i))))
              bb))
          (byte-range- [begin end seq]
            (byte-range begin end (byte-buffer seq)))
          (char-range- [begin end size]
            (char-range begin end (CharBuffer/allocate size)))
          (?write= [cr br string]
            (let [ccr (.clone cr)]
              (.writeBytes cr br (Charset/forName "UTF8"))
              (?= (seq ccr) (seq string))))]
    (let [br (byte-range- 0 10 (map int "0123456789"))
          cr (char-range- 0 10 10)]
      (?write= cr br "0123456789")
      (?range= br [10 10])
      (?range= cr [10 10]))
    (let [br (byte-range- 1 3 (map int "01234"))
          cr (char-range- 4 8 10)]
      (?write= cr br "12\0\0")
      (?range= br [3 3])
      (?range= cr [6 8]))
    (let [br (byte-range- 1 9 (map int "0123456789"))
          cr (char-range- 3 5 10)]
      (?write= cr br "12")
      (?range= br [3 9])
      (?range= cr [5 5]))))

;;bytes to chars errors
;char range
;;converting to/from bytes
;;;conversion methods
;;;convertors range(CharSequence csq)
        

;ranges for byte[] and char[]ap(CharSequence csq)






