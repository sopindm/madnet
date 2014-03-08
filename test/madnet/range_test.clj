(ns madnet.range-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c] 
            [madnet.range :as r])
  (:import [madnet.range Range IntegerRange ProxyRange]
           [madnet.channel Result]))

(defmacro ?range= [expr [begin end]]
  `(let [^Range range# ~expr]
     (?= (.begin range#) ~begin)
     (?= (.end range#) ~end)))

(defn irange ^IntegerRange [min max]
  (IntegerRange. min max))

(deftest making-range
  (?range= (irange 5 15) [5 15])
  (?range= (irange 5 5) [5 5])
  (?throws (irange 6 5) IllegalArgumentException)
  (?= (r/size (irange 5 10)) 5))

(deftest closing-irange
  (let [r (irange 0 10)]
    (?true (c/open? r))))

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

(defn srange ^madnet.range.Range [begin end coll]
  (let [coll (atom coll)
        writer (fn [^Range dst ^Range src]
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
      (seq [] (seq (->> @coll (take (.end ^IntegerRange this)) (drop (.begin ^IntegerRange this)))))
      (count [] (r/size this))
      (write [src] (writer this src))
      (read [ts] nil)
      (iterator [] (.iterator (or (seq this) []))))))

(defn another-range ^ProxyRange [begin end coll]
  (let [range (srange begin end coll)]
    (r/proxy range
      (writeImpl [ts] nil)
      (readImpl [ts]
         (let [^Range writer (if (isa? (type ts) ProxyRange) (.range ^ProxyRange ts) ts)]
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
