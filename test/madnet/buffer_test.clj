(ns madnet.buffer-test
  (:require [khazad-dum.core :refer :all]
            [madnet.range :as r]
            [madnet.buffer :as b :use [buffer]]
            [madnet.range-test :refer :all])
  (:import [madnet.range]))

(deftest making-simple-buffer
  (let [b (b/buffer 100)]
    (?= (count b) 100)
    (?range= (b 12 50) [12 50])
    (?range= (b 15) [0 15])
    (?range= (b) [0 100])
    (?range= (b 0 80) [0 80])
    (r/write (b 0 100) (object-range 0 100 (range 100)))
    (?= (seq b) (seq (range 100)))))

(deftest making-circular-buffers
  (?false (b/circular? (buffer 10)))
  (?true (b/circular? (buffer 10 :circular true)))
  (?false (b/circular? (buffer 10 :circular false)))
  (let [b (buffer 10 :circular true)]
    (?range= (r/expand 5 (b 8 10)) [8 5])))

(deftest making-buffers-with-element-type
  (let [b (buffer 10 :element :object)]
    (r/write (b) (object-range 0 5 (map #(format "%s" %) (range -2 3))))
    (?= (seq b) ["-2" "-1" "0" "1" "2" nil nil nil nil nil]))
  (let [b (buffer 5 :element :byte)]
    (?range= (b) [0 5])
    (?true (isa? (type (b)) madnet.range.nio.ByteRange)))
  (let [b (buffer 10 :element :char)]
    (?range= (b) [0 10])
    (?true (isa? (type (b)) madnet.range.nio.CharRange)))
  (?throws (buffer 10 :element :unknown) IllegalArgumentException))

(deftest direct-and-indirect-nio-buffers
  (?= (direct? (buffer 10)) nil)
  (?false (direct? (buffer 10 :element :byte)))
  (?true (direct? (buffer 10 :element :byte :direct true)))
  (?false (-> ((buffer 10 :element :byte))
              .buffer .isDirect))
  (?throws (buffer 10 :direct true) IllegalArgumentException)
  (?throws (buffer 10 :direct false) IllegalArgumentException)
  (?throws (buffer 10 :element :char :direct true)
           IllegalArgumentException))

(deftest wrong-option-error
  (?throws (buffer 10 :unknown true) IllegalArgumentException)
  (?throws (buffer 10 :element :byte :unknown true)
           IllegalArgumentException)
  (?throws (buffer 10 :element :char :unknown true)
           IllegalArgumentException))

(deftest making-buffer-from-existing-collection
  (let [b (wrap (range 10))]
    (?= (count b) 10)
    (?= (seq b) [0 1 2 3 4 5 6 7 8 9]))
  (?throws (wrap (range 10) :element :byte) IllegalArgumentException))

(deftest making-buffer-from-byte-array
  (let [b (wrap (byte-array (map byte (range 10))))]
    (?= (count b) 10)
    (?= (seq b) [0 1 2 3 4 5 6 7 8 9])
    (?true (isa? (type (b)) madnet.range.nio.ByteRange))
    (?false (direct? b)))
  (?throws (wrap (byte-array 10) :direct true) IllegalArgumentException))

(deftest making-buffer-from-char-array
  (let [b (wrap (char-array (map char (range (int \0) (int \5)))))]
    (?= (count b) 5)
    (?= (seq b) (seq "01234"))
    (?true (isa? (type (b)) madnet.range.nio.CharRange))
    (?false (direct? b))))

(deftest buffer-options
  (?= (options (buffer 10)) {:element :object :circular false})
  (?= (options (buffer 10 :circular true))
      {:element :object :circular true})
  (?= (options (buffer 10 :element :char))
      {:element :char :circular false :direct false})
  (?= (options (buffer 10 :element :byte :direct true))
      {:element :byte :circular false :direct true}))

;cloning buffers



