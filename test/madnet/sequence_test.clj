(ns madnet.sequence-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c]
            [madnet.buffer :as b]
            [madnet.sequence :as s]
            [madnet.range-test :refer :all])
  (:import [madnet.range IntegerRange ObjectRange]))

(deftest making-sequence
  (let [s (s/sequence 100)]
    (?= (count s) 0)
    (?= (s/size s) 0)
    (?= (s/free s) 100)
    (?range= (s/reader s) [0 0])
    (?range= (s/writer s) [0 100])
    (?true (s/circular? s))
    (?= (count (.buffer s)) 100)
    (?= (b/options (.buffer s)) {:element :object :circular true})))

(deftest making-sequence-with-buffer-options
  (let [s (s/sequence [100 :element :byte :circular false])]
    (?= (b/options (.buffer s))
        {:element :byte :circular false :direct false})))

(deftest making-sequence-with-buffer
  (let [b (b/buffer 100 {:element :char})
        s (s/sequence b)]
    (?= (b/options (.buffer s))
        {:element :char :circular false :direct false})))

(deftest making-sequence-with-reader-and-writer
  (let [s (s/sequence [10 :circular false] :reader [5 8])]
    (?range= (s/reader s) [5 8])
    (?range= (s/writer s) [8 10]))
  (let [s (s/sequence [10 :circular false] :writer [1 3])]
    (?range= (s/writer s) [1 3])
    (?range= (s/reader s) [0 1]))
  (let [s (s/sequence [10 :circular true] :reader [3 5])]
    (?range= (s/reader s) [3 5])
    (?range= (s/writer s) [5 3]))
  (let [s (s/sequence [10 :circular true] :writer [1 8])]
    (?range= (s/reader s) [8 1])
    (?range= (s/writer s) [1 8])))

;sequence seq

(deftest reading-from-sequence
  (let [s (s/sequence (b/wrap (range -5 5)) :reader [0 10])
        r ((b/buffer 10))
        rc (.clone r)]
    (?range= (.reader (c/read! s r)) [10 10])
    (?range= r [10 10])
    (?range= (s/reader s) [10 10])
    (?range= (s/writer s) [10 10])
    (?= (seq rc) (seq (range -5 5)))
    (?= (.read s ((b/buffer 10 {:element :byte}))) nil)))

(deftest writing-to-sequence
  (let [s (s/sequence [10 :element :byte])
        r ((b/wrap (byte-array (map byte (range 10)))))
        rc (.clone r)]
    (c/write! s r)
    (?range= (s/reader s) [0 10])
    (?range= (s/writer s) [10 10])
    (?range= r [10 10])
    (?= (seq s) (seq (map byte (range 10)))))) 

;reader and writer linking

;wrapped sequencies

;writing sequence to sequence

;operating with reader and writer

;reading from sequence
;writing to sequence
;making no-circular sequence
;sequence seq
;sequence iterator
