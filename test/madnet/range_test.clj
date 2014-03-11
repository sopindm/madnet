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
           UnsupportedOperationException)
  (?throws (c/push! (irange 0 10) []) UnsupportedOperationException)
  (?throws (c/push! (irange 0 10) [] :timeout 0) UnsupportedOperationException)
  (?throws (c/push! (irange 0 10) [] :timeout 10) UnsupportedOperationException)
  (?throws (c/peek! (irange 0 10)) UnsupportedOperationException)
  (?throws (c/peek! (irange 0 10) :timeout 0) UnsupportedOperationException)
  (?throws (c/peek! (irange 0 10) :timeout 10) UnsupportedOperationException))

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
      (tryPush [obj]
        (boolean (when (pos? (count this))
                   (swap! coll #(concat (take (.begin this) %)
                                        [obj]
                                        (drop (inc (.begin this)) %)))
                   (r/drop! 1 this)
                   true)))
      (tryPeek []
        (when (pos? (count this))
          (let [result (nth @coll (.begin this))]
            (r/drop! 1 this)
            result)))
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

(deftest pushing-item-to-range
  (let [r (srange 0 5 (repeat 5 nil))
        rc (.clone r)]
    (?= (c/push! r 1) r)
    (?range= r [1 5])
    (?= (first (seq rc)) 1)
    (?= (c/push! r 2 :timeout 0) r)
    (?range= r [2 5])
    (?= (second (seq rc)) 2)
    (?= (c/push! r 3 :timeout 10) r)
    (?range= r [3 5])
    (?= (nth (seq rc) 2) 3)))

(deftest pushing-item-to-full-range-with-zero-timeout
  (let [r (srange 3 3 (repeat 5 nil))]
    (?= (c/push! r 1 :timeout 0) nil)))

(deftest pushing-item-to-full-range
  (let [r (srange 3 3 (repeat 5 nil))
        rc (.clone r)]
    (let [f (future (c/push! r 1))]
      (Thread/sleep 2)
      (?false (realized? f))
      (r/expand! 1 r)
      (Thread/sleep 1)
      (?true (realized? f))
      (?range= r [4 4])
      (?= (first (seq (r/expand 1 rc))) 1)
      (future-cancel f))))

(deftest pushing-item-to-full-range-with-timeout
  (let [r (srange 3 3 (repeat 5 nil))
        rc (.clone r)]
    (let [f (future (c/push! r 1 :timeout 3))]
      (Thread/sleep 1)
      (?false (realized? f))
      (Thread/sleep 4)
      (?true (realized? f))
      (?range= r [3 3]))
    (let [f (future (c/push! r 1 :timeout 1000))]
      (Thread/sleep 2)
      (?false (realized? f))
      (r/expand! 1 r)
      (Thread/sleep 1)
      (?true (realized? f))
      (?= (first (seq (r/expand 1 rc))) 1)
      (?range= r [4 4])
      (?= @f r))))

(deftest peeking-from-range
  (let [r (srange 0 3 (range 3))]
    (?= (c/peek! r) 0)
    (?= (c/peek! r :timeout 0) 1)
    (?= (c/peek! r :timeout 1000) 2)))

(deftest peeking-from-empty-range-with-zero-timeout
  (let [r (srange 1 1 (range 10))]
    (?= (c/peek! r :timeout 0) nil)))

(deftest peeking-from-empty-range
  (let [r (srange 3 3 (range 5))
        f (future (c/peek! r))]
    (Thread/sleep 2)
    (?false (realized? f))
    (r/expand! 1 r)
    (Thread/sleep 1)
    (?true (realized? f))
    (?range= r [4 4])
    (?= @f 3)
    (future-cancel f)))

(deftest peeking-item-from-empty-range-with-timeout
  (let [r (srange 3 3 (range 5))]
    (let [f (future (c/peek! r :timeout 3))]
      (Thread/sleep 1)
      (?false (realized? f))
      (Thread/sleep 4)
      (?true (realized? f))
      (?range= r [3 3])
      (?= @f nil))
    (let [f (future (c/peek! r :timeout 1000))]
      (Thread/sleep 2)
      (?false (realized? f))
      (r/expand! 1 r)
      (Thread/sleep 1)
      (?true (realized? f))
      (?range= r [4 4])
      (?= @f 3))))



