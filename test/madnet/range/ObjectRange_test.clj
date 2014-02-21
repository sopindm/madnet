(ns madnet.range.ObjectRange-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c] 
            [madnet.range :as r]
            [madnet.range-test :use [irange]])
  (:import [madnet.range IntegerRange ObjectRange]
           [madnet.channel Result]))

(defn object-range ^ObjectRange [begin end ^java.util.Collection content]
  (ObjectRange. begin end (java.util.ArrayList. content)))

(deftest making-object-range
  (let [r (object-range 3 15 (repeat 15 nil))]
    (?range= r [3 15]))
  (let [r (object-range 2 12 (range 15))]
    (?= (seq r) (seq (range 2 12)))
    (?range= r [2 12])))

(deftest overexpanding-object-range
  (let [r (object-range 0 10 (repeat 10 nil))]
    (?throws (r/expand! 1 r) IllegalArgumentException))
  (?throws (object-range 0 10 (repeat 5 nil)) IllegalArgumentException)
  (?throws (object-range -1 10 (repeat 10 nil)) IllegalArgumentException))

(deftest cloning-object-range
  (let [r (object-range 0 10 (range 10))
        rc (.clone r)]
    (r/take! 5 r)
    (?= (seq rc) (seq (range 10)))))

(deftest writing-to-object-range
  (let [src (object-range 0 10 (range 10))
        dst (object-range 0 5 (repeat 5 nil))
        dst-clone (.clone dst)]
    (c/write! dst src)
    (?range= dst [5 5])
    (?range= src [5 10])
    (?= (seq dst-clone) (seq (range 5))))
  (let [src (object-range 0 5 (range 5))
        dst (object-range 0 10 (repeat 10 nil))
        dst-clone (.clone dst)]
    (c/write! dst src)
    (?range= dst [5 10])
    (?range= src [5 5])
    (?= (seq dst-clone) (seq (concat (range 5) (repeat 5 nil))))))

(deftest accessing-object-range
  (let [r (object-range 2 8 (range 10))]
    (?= (.get r 2) 4)
    (.set r 5 -100)
    (?= (.get r 5) -100)
    (?throws (.get r -1) IllegalArgumentException)
    (?throws (.set r -1 100500) IllegalArgumentException)
    (?throws (.get r 7) IllegalArgumentException)
    (?throws (.set r 7 100500) IllegalArgumentException)))

(deftest reading-anything-iterable-to-object-range
  (let [r (object-range 0 10 (repeat 10 nil))
        rc (.clone r)
        ar (srange 0 5 (range 5))]
    (?= (c/write! r ar) (Result. 5 5))
    (?range= ar [5 5])
    (?= (seq rc) (seq (concat (range 5) (repeat 5 nil))))))
