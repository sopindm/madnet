(ns madnet.range.nio-test
  (:require [khazad-dum :refer :all]
            [madnet.range-test :refer :all]
            [madnet.channel :as c]
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

(defn- nrange ^madnet.range.nio.Range [begin end buffer]
   (Range. begin end buffer))

(deftest making-nio-range
  (let [b (ByteBuffer/allocate 1024)
        r (nrange 128 512 b)]
    (?range= r [128 512])
    (?buffer= (.buffer r) 128 512 1024)
    (?true (c/open? r))))

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

(defn byte-range ^madnet.range.nio.ByteRange [begin end buffer]
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
    (operation-check c/write!)
    (operation-check #(c/read! %2 %1))))

(deftest writing-more-and-less-to-byte-buffer
  (let [b (ByteBuffer/allocate 10)
        source (byte-range 0 10 b)
        dest (byte-range 0 5 (ByteBuffer/allocate 10))
        dest-clone (.clone dest)]
    (dotimes [i 10]
      (.put b i (+ i 3)))
    (c/write! dest source)
    (?range= source [5 10])
    (?range= dest [5 5])
    (?= (seq dest-clone) [3 4 5 6 7]))
  (let [b (ByteBuffer/allocate 10)
        source (byte-range 0 5 b)
        dest (byte-range 0 10 (ByteBuffer/allocate 10))
        dest-clone (.clone dest)]
    (dotimes [i 5]
      (.put b i (+ i 2)))
    (c/write! dest source)
    (?range= source [5 5])
    (?range= dest [5 10])
    (?= (seq (r/take 5 dest-clone)) [2 3 4 5 6])))

;;
;; Char Range
;;

(defn- char-range ^madnet.range.nio.CharRange [begin end buffer]
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

(deftest writing-char-range-to-char-range
  (letfn [(char-range- [begin end chars]
            (char-range begin end (CharBuffer/wrap (char-array chars))))
          (?write= [^madnet.channel.IChannel dst src str]
            (let [clone (.clone dst)]
              (c/write! dst src)
              (?= (seq clone) (seq str))))]
    (let [src (char-range- 0 5 "Hello")
          dst (char-range- 0 5 5)]
      (?write= dst src "Hello")
      (?range= dst [5 5])
      (?range= src [5 5]))
    (let [src (char-range- 0 2 "Hi")
          dst (char-range- 0 5 5)]
      (?write= dst src "Hi\0\0\0")
      (?range= dst [2 5])
      (?range= src [2 2]))
    (let [src (char-range- 0 5 "Hello")
          dst (char-range- 1 5 5)]
      (?write= dst src "Hell")
      (?range= dst [5 5])
      (?range= src [4 5]))))

(letfn [(byte-buffer [seq]
          (let [bb (ByteBuffer/allocate (count seq))]
            (dotimes [i (count seq)]
              (.put bb i (byte (nth seq i))))
            bb))
        (char-buffer [seq]
          (let [cb (CharBuffer/allocate (count seq))]
            (dotimes [i (count seq)]
              (.put cb i (nth seq i)))
            cb))
        (byte-range- [begin end seq]
          (byte-range begin end (byte-buffer seq)))
        (char-range- [begin end seq]
          (char-range begin end (char-buffer seq)))
        (?write= [^madnet.range.nio.CharRange cr br string]
          (let [ccr (.clone cr)]
            (.writeBytes cr br (Charset/forName "UTF8"))
            (?= (seq ccr) (seq string))))
        (?read= [^madnet.range.nio.CharRange cr ^madnet.channel.IChannel br bytes]
          (let [cbr (.clone br)]
            (.readBytes cr br (Charset/forName "UTF8"))
            (?= (seq cbr) bytes)))]
  (deftest bytes-to-chars-conversion
    (let [br (byte-range- 0 10 (map int "0123456789"))
          cr (char-range- 0 10 (repeat 10 (char 0)))]
      (?write= cr br "0123456789")
      (?range= br [10 10])
      (?range= cr [10 10]))
    (let [br (byte-range- 1 3 (map int "01234"))
          cr (char-range- 4 8 (repeat 10 (char 0)))]
      (?write= cr br "12\0\0")
      (?range= br [3 3])
      (?range= cr [6 8]))
    (let [br (byte-range- 1 9 (map int "0123456789"))
          cr (char-range- 3 5 (repeat 10 (char 0)))]
      (?write= cr br "12")
      (?range= br [3 9])
      (?range= cr [5 5])))
  (deftest chars-to-bytes-conversion
    (let [br (byte-range- 0 10 (repeat 10 0))
          cr (char-range- 0 10 "abcdefghij")]
      (?read= cr br [97 98 99 100 101 102 103 104 105 106])
      (?range= br [10 10])
      (?range= cr [10 10]))
    (let [br (byte-range- 0 2 (repeat 2 0))
          cr (char-range- 0 5 "abcde")]
      (?read= cr br [97 98])
      (?range= br [2 2])
      (?range= cr [2 5]))
    (let [br (byte-range- 0 5 (repeat 5 0))
          cr (char-range- 0 2 "abcde")]
      (?read= cr br [97 98 0 0 0])
      (?range= br [2 5])
      (?range= cr [2 2])))
  (deftest error-converting-bytes-to-chars
    (?throws (.writeBytes ^madnet.range.nio.CharRange (char-range- 0 10 (repeat 10 (char 0)))
                          (byte-range- 0 1 [-1])
                          (Charset/forName "UTF8"))
             java.nio.charset.CharacterCodingException)))







