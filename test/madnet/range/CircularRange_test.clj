(ns madnet.range.CircularRange-test
  (:require [khazad-dum :refer :all]
            [madnet.range-test :refer [irange ?range= srange another-range]]
            [madnet.channel :as c] 
            [madnet.range :as r])
  (:import [madnet.range Range IntegerRange CircularRange ProxyRange ObjectRange]
           [madnet.channel Result]))

(defn- crange ^CircularRange [min max limit]
  (CircularRange. (irange min max) limit))

(deftest making-circular-ranges
  (let [r (irange 5 15)
        cr (crange 10 15 r)]
    (r/expand! 1 cr)
    (?range= cr [10 6])
    (?range= (.limit cr) [5 15])
    (?= (r/size cr) 6)
    (?true (c/readable? cr))
    (?true (c/writeable? cr))
    (?throws (crange 5 10 (irange 6 10)) IllegalArgumentException)
    (?throws (crange 5 10 (irange 5 9)) IllegalArgumentException)))

(deftest circular-range-cloning
  (let [cr1 (crange 1 2 (irange 0 4))
        cr2 (.clone cr1)]
    (?range= cr2 [1 2])
    (?range= (.limit cr2) [0 4])
    (r/take! 1 cr1)
    (?range= cr2 [1 2])
    (r/take! 1 (.limit cr1))
    (?range= (.limit cr2) [0 4])))

(deftest closing-circular-range
  (let [cr (crange 0 5 (irange 0 5))]
    (c/close! cr :read)
    (?false (c/readable? cr))
    (?true (c/writeable? cr))
    (c/close! cr :write)
    (?false (c/writeable? cr))))

(deftest circular-range-operations
  (let [cr (crange 0 0 (irange -5 5))]
    (?range= (r/expand! 7 cr) [0 -3])
    (?range= (r/take 6 cr) [0 -4])
    (?range= (r/drop 6 cr) [-4 -3])
    (?range= (r/take-last 1 cr) [-4 -3])
    (?range= (r/drop-last 2 cr) [0 5])
    (?range= (r/expand 3 cr) [0 0])
    (?= (r/size (r/expand 3 cr)) 10)
    (?throws (r/expand 4 cr) IndexOutOfBoundsException)
    (?throws (r/take 100 cr) IndexOutOfBoundsException)
    (?throws (r/take-last 100 cr) IndexOutOfBoundsException)
    (?throws (r/drop 100 cr) IndexOutOfBoundsException)
    (?throws (r/drop-last 100 cr) IndexOutOfBoundsException)))

(deftest circular-range-first-and-rest
  (let [cr1 (crange 0 10 (irange -5 15))]
    (?range= (.first cr1) [0 10])
    (?range= (.dropFirst cr1) [10 10])
    (?range= (.limit cr1) [-5 15]))
  (let [cr2 (crange 8 10 (irange 0 10))]
    (r/expand! 3 cr2)
    (?range= (.first cr2) [8 10])
    (?range= (.dropFirst cr2) [0 3])
    (?range= (.first cr2) [0 3])))

(defn- circular ^CircularRange [generator begin end coll]
  (if (<= begin end)
    (proxy [CircularRange clojure.lang.Seqable clojure.lang.Counted]
        [(generator begin end coll) (irange 0 (count coll))]
      (seq [] (seq (concat (seq (.first ^CircularRange this))
                           (seq (-> ^CircularRange this .clone .dropFirst .first)))))
      (count [] (count (seq this))))
    (let [cr (circular generator begin (count coll) coll)]
      (.expand cr  ^int end)
      cr)))

(deftest reading-and-writing-from-circular-ranges
  (let [r1 (circular srange 3 1 (repeat 5 nil))
        rc (.clone r1)
        r2 (circular srange 2 1 [1 2 3 4])]
    (?= (c/write! r1 r2) (Result. 3 3))
    (?range= r1 [1 1])
    (?range= r2 [1 1])
    (?= (seq rc) [3 4 1]))
  (let [r1 (circular another-range 3 1 (repeat 5 nil))
        rc (.clone r1)
        r2 (circular another-range 4 2 (range 6))]
    (?= (c/write! r1 r2) (Result. 3 3))
    (?range= r1 [1 1])
    (?range= r2 [1 2])
    (?= (seq rc) [4 5 0])))

(deftest circular-range-iterator
  (?= (seq (circular srange 3 1 (range 5))) [3 4 0]))

