(ns madnet.range-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c] 
            [madnet.range :as r])
  (:import [madnet.range Range IntegerRange CircularRange
                         ProxyRange LinkedRange ObjectRange]
           [madnet.channel Result]))

(defmacro ?range= [expr [begin end]]
  `(let [range# ~expr]
     (?= (.begin range#) ~begin)
     (?= (.end range#) ~end)))

(defn irange [min max]
  (IntegerRange. min max))

(deftest making-range
  (?range= (irange 5 15) [5 15])
  (?range= (irange 5 5) [5 5])
  (?throws (irange 6 5) IllegalArgumentException)
  (?= (r/size (irange 5 10)) 5))

(deftest range-read-and-write
  (?throws (c/read! (irange 0 1) (irange 5 10))
           UnsupportedOperationException)
  (?throws (c/write! (irange 2 10) (irange 5 10))
           UnsupportedOperationException))

(deftest range-mutable-take-drop-and-expand
  (let [r (irange 5 10)]
    (?range= (r/take! 3 r) [5 8])
    (?range= r [5 8])
    (?range= (r/drop! 1 r) [6 8])
    (?range= r [6 8])
    (?range= (r/expand! 5 r) [6 13])
    (?range= r [6 13])
    (?throws (r/take! 10 r) IndexOutOfBoundsException)
    (?throws (r/drop! 10 r) IndexOutOfBoundsException)
    (?range= (r/drop-last! 2 r) [6 11])
    (?range= (r/take-last! 3 r) [8 11])
    (?throws (r/take-last! 10 r) IndexOutOfBoundsException)
    (?throws (r/drop-last! 10 r) IndexOutOfBoundsException)))

(deftest range-cloning
  (let [r (irange 5 10)
        c (.clone r)]
    (r/drop! 1 r)
    (?range= r [6 10])
    (?range= c [5 10])))

(deftest mutable-range-split
  (let [r (irange 5 10)
        s (r/split! 3 r)]
    (?range= r [8 10])
    (?range= s [5 8])))

(deftest immutable-range-operations
  (let [r (irange 5 10)]
    (?range= (r/take 3 r) [5 8])
    (?range= r [5 10])
    (?range= (r/take-last 3 r) [7 10])
    (?range= r [5 10])
    (?range= (r/drop 2 r) [7 10])
    (?range= r [5 10])
    (?range= (r/drop-last 2 r) [5 8])
    (?range= r [5 10])
    (?range= (r/expand 5 r) [5 15])
    (?range= r [5 10])
    (let [[spliten rest] (r/split 3 r)]
      (?range= spliten [5 8])
      (?range= rest [8 10])
      (?range= r [5 10]))))

(defn- crange [min max limit]
  (CircularRange. (irange min max) limit))

(deftest making-circular-ranges
  (let [r (irange 5 15)
        cr (crange 10 15 r)]
    (r/expand! 1 cr)
    (?range= cr [10 6])
    (?range= (.limit cr) [5 15])
    (?= (r/size cr) 6)
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

(defn srange [begin end coll]
  (let [coll (atom coll)
        writer (fn [dst src]
                 (if (isa? (type src) clojure.lang.Seqable)
                   (let [write-size (min (count dst) (count src))]
                     (reset! coll (concat (take (.begin dst) @coll)
                                          (take write-size (seq src))
                                          (drop (+ (.begin dst)
                                                   write-size) @coll)))
                     (r/drop! write-size src)
                     (r/drop! write-size dst)
                     (Result. write-size write-size))))]
    (proxy [IntegerRange clojure.lang.Seqable clojure.lang.Counted]
           [begin end]
      (seq [] (seq (->> @coll (take (.end this)) (drop (.begin this)))))
      (count [] (r/size this))
      (write [src] (writer this src))
      (read [ts] nil)
      (iterator [] (.iterator (seq this))))))

(defn- another-range [begin end coll]
  (let [range (srange begin end coll)]
    (r/proxy range
      (write [ts] nil)
      (read [ts]
         (let [writer (if (isa? (type ts) ProxyRange) (.range ts) ts)]
           (or (.read range writer) (.write writer range)))))))

(deftest simple-seq-based-range
  (let [r (srange 2 5 (range 10))]
    (?range= r [2 5])
    (?= (seq r) [2 3 4])
    (let [r2 (srange 0 4 (repeat 10 nil))
          cr2 (.clone r2)]
      (?= (c/read! r r2) (Result. 3 3))
      (?range= r [5 5])
      (?range= r2 [3 4])
      (?= (seq r2) [nil])
      (?= (seq cr2) [2 3 4 nil]))
    (?= (c/read! (srange 0 5 (range 5)) (srange 0 4 (repeat 4 nil)))
        (Result. 4 4))))

(deftest writing-without-write-impl
  (let [r1 (another-range 1 6 (repeat 6 nil))
        rc (.clone r1)
        r2 (another-range 0 5 [1 2 3 4 5])]
    (?= (c/write! r1 r2) (Result. 5 5))
    (?= (seq (.range rc)) [1 2 3 4 5])))

(deftest reading-and-writing-to-unknown-type
  (let [r1 (srange 0 10 (repeat 10 nil))
        r2 (irange 0 10)]
    (?throws (c/write! r2 r1) UnsupportedOperationException)
    (?throws (c/write! r1 r2) UnsupportedOperationException)
    (?throws (c/read! r1 r2) UnsupportedOperationException)
    (?throws (c/read! r2 r1) UnsupportedOperationException)))

(deftest immutable-write-and-read
  (let [r1 (srange 0 5 (repeat 5 nil))
        r2 (srange 0 3 [1 2 3])]
    (let [[writen read] (c/write r1 r2)]
      (?range= writen [3 5])
      (?range= read [3 3]))
    (?= (seq r1) [1 2 3 nil nil])
    (?= (seq r2) [1 2 3])
    (let [[read writen] (c/read (r/drop! 2 r1) r2)]
      (?range= read [5 5])
      (?range= writen [3 3])
      (?= (seq r1) [3 nil nil])
      (?= (seq r2) [3 nil nil]))))

(defn- circular [generator begin end coll]
  (if (<= begin end)
    (proxy [CircularRange clojure.lang.Seqable clojure.lang.Counted]
        [(generator begin end coll) (irange 0 (count coll))]
      (seq [] (seq (concat (seq (.first this))
                           (seq (-> this .clone .dropFirst .first)))))
      (count [] (count (seq this))))
    (doto (circular generator begin (count coll) coll)
      (.expand end))))

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

