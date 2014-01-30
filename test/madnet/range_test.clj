(ns madnet.range-test
  (:require [khazad-dum.core :refer :all]
            [madnet.range :as r])
  (:import [madnet.range Range CircularRange ProxyRange LinkedRange]))

(defmacro ?range= [expr [begin end]]
  `(let [range# ~expr]
     (?= (.begin range#) ~begin)
     (?= (.end range#) ~end)))

(defn- irange [min max]
  (Range. (int min) (int max)))

(deftest making-range
  (?range= (irange 5 15) [5 15])
  (?range= (irange 5 5) [5 5])
  (?throws (irange 6 5) IllegalArgumentException)
  (?= (r/size (irange 5 10)) 5))

(deftest range-read-and-write
  (?throws (r/read! (irange 0 1) (irange 5 10)) UnsupportedOperationException)
  (?throws (r/write! (irange 2 10) (irange 5 10)) UnsupportedOperationException))

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
  (CircularRange. (int min) (int max) limit))

(deftest making-circular-ranges
  (let [r (irange 5 15)
        cr (crange 10 6 r)]
    (?range= cr [10 6])
    (?range= (.limit cr) [5 15])
    (r/take! 5 r)
    (?range= (.limit cr) [5 15])
    (r/take! 5 (.limit cr))
    (?range= (.limit cr) [5 15])
    (?= (r/size cr) 6)
    (?throws (crange 5 10 (irange 6 10)) IllegalArgumentException)
    (?throws (crange 5 10 (irange 5 9)) IllegalArgumentException)))

(deftest circular-range-cloning
  (let [cr1 (crange 1 2 (irange 0 4))
        l1 (.limit cr1)
        cr2 (.clone cr1)]
    (?range= cr2 [1 2])
    (?range= (.limit cr2) [0 4])
    (r/take! 1 (.limit cr1))
    (?range= (.limit cr1) [0 4])
    (?range= (.limit cr2) [0 4])))

(deftest circular-range-operations
  (let [cr (crange 0 0 (irange -5 5))]
    (?range= (r/expand! 7 cr) [0 -3])
    (?range= (r/take 6 cr) [0 -4])
    (?range= (r/drop 6 cr) [-4 -3])
    (?range= (r/take-last 1 cr) [-4 -3])
    (?range= (r/drop-last 2 cr) [0 -5])
    (?throws (r/take 100 cr) IndexOutOfBoundsException)
    (?throws (r/take-last 100 cr) IndexOutOfBoundsException)
    (?throws (r/drop 100 cr) IndexOutOfBoundsException)
    (?throws (r/drop-last 100 cr) IndexOutOfBoundsException)))

(deftest circular-range-ranges
  (let [cr1 (crange 0 10 (irange -5 15))
        cr2 (crange 8 3 (irange 0 10))]
    (?= (count (.ranges cr1)) 1)
    (?range= (first (.ranges cr1)) [0 10])
    (?range= (-> cr1 .ranges first .limit) [-5 15])
    (?= (count (.ranges cr2)) 2)
    (?range= (-> cr2 .ranges first) [8 10])
    (?range= (-> cr2 .ranges first .limit) [0 10])
    (?range= (-> cr2 .ranges second) [0 3])
    (?range= (-> cr2 .ranges second .limit) [0 10])))

(deftest range-proxy-making
  (let [r (irange 0 10)
        p (r/proxy r)]
    (?= (r/size p) 10)
    (?range= (.range p) [0 10])))
    
(deftest range-proxy-cloning
  (let [r (irange 0 10)
        pr (r/proxy r)
        prc (.clone pr)]
    (?range= (.range prc) [0 10])
    (r/drop! 5 r)
    (?range= (.range prc) [0 10])))

(deftest range-proxy-operations
  (let [pr (r/proxy (irange 0 10))]
    (?= (r/size (r/drop! 3 pr)) 7)
    (?range= (.range pr) [3 10])
    (?= (r/size (r/take! 5 pr)) 5)
    (?range= (.range pr) [3 8])
    (?= (r/size (r/expand! 3 pr)) 8)
    (?range= (.range pr) [3 11])
    (?= (r/size (r/take-last! 4 pr)) 4)
    (?range= (.range pr) [7 11])
    (?= (r/size (r/drop-last! 1 pr)) 3)
    (?range= (.range pr) [7 10])))

(deftest range-proxy-read-and-write
  (let [r (proxy [Range] [0 10]
            (write [seq] (throw (UnsupportedOperationException. "Writing to proxies is OK")))
            (read [seq] (throw (UnsupportedOperationException. "Reading from proxies is OK"))))
        pr (r/proxy r)]
    (?throws (r/write! pr nil) UnsupportedOperationException "Writing to proxies is OK")
    (?throws (r/read! pr nil) UnsupportedOperationException "Reading from proxies is OK")))

(deftest read-only-and-write-only-proxies
  (let [r (proxy [Range] [0 10] (write [seq] nil) (read [seq] nil))
        rp (r/proxy r :read-only)
        wp (r/proxy r :write-only)]
    (?= (r/read! rp nil) rp)
    (?throws (r/read! wp nil) UnsupportedOperationException)
    (?= (r/write! wp nil) wp)
    (?throws (r/write! rp nil) UnsupportedOperationException)))

(deftest extending-range-proxy
  (let [r (irange 0 10)
        p (r/proxy r (expand [n] this))]
    (?range= (.range (r/expand 1000 p)) [0 10])))

(defn- srange [begin end coll]
  (let [coll (atom coll)]
    (proxy [CircularRange clojure.lang.Seqable clojure.lang.Counted]
        [begin end (irange 0 (count @coll))]
      (seq [] (seq (mapcat #(->> @coll (take (.end %)) (drop (.begin %))) (.ranges this))))
      (count [] (r/size this))
      (write [ts] 
        (if (isa? (type ts) clojure.lang.Seqable)
          (letfn [(write- [dst src]
                    (let [write-size (min (count dst) (count src))]
                      (reset! coll (concat (take (.begin dst) @coll)
                                           (take write-size (seq src))
                                           (drop (+ (.begin dst) write-size) @coll)))
                      (r/drop! write-size dst)
                      (r/drop! write-size src)
                      write-size))]
            (let [write-size (reduce (fn [size r] (+ size (write- r ts))) 0 (.ranges this))]
              (r/drop! write-size this)))))
      (read [ts] nil))))

(defn- another-range [begin end coll]
  (let [sr (srange begin end coll)]
    (r/proxy sr
      (write [ts] nil)
      (read [ts] (if (isa? (type ts) ProxyRange)
                   (do (r/read! sr (.range ts)) this)
                   (do (r/read! sr ts) this))))))

(deftest simple-seq-based-range
  (let [r (srange 2 5 (range 10))]
    (?range= r [2 5])
    (?= (seq r) [2 3 4])
    (let [r2 (srange 0 4 (repeat 10 nil))
          cr2 (.clone r2)]
      (?range= (r/read! r r2) [5 5])
      (?range= r2 [3 4])
      (?= (seq r2) [nil])
      (?= (seq cr2) [2 3 4 nil]))))

(deftest writing-without-write-impl
  (let [r1 (another-range 1 6 (repeat 6 nil))
        rc (.clone r1)
        r2 (another-range 0 5 [1 2 3 4 5])]
    (?= (r/write! r1 r2) r1)
    (?= (seq (.range rc)) [1 2 3 4 5])))

(deftest reading-and-writing-to-unknown-type
  (let [r1 (srange 0 10 (repeat 10 nil))
        r2 (irange 0 10)]
    (?throws (r/write! r2 r1) UnsupportedOperationException)
    (?throws (r/write! r1 r2) UnsupportedOperationException)
    (?throws (r/read! r1 r2) UnsupportedOperationException)
    (?throws (r/read! r2 r1) UnsupportedOperationException)))

(deftest immutable-write-and-read
  (let [r1 (srange 0 5 (repeat 5 nil))
        r2 (srange 0 3 [1 2 3])]
    (let [[writen read] (r/write r1 r2)]
      (?range= writen [3 5])
      (?range= read [3 3]))
    (?= (seq r1) [1 2 3 nil nil])
    (?= (seq r2) [1 2 3])
    (let [[read writen] (r/read (r/drop! 2 r1) r2)]
      (?range= read [5 5])
      (?range= writen [3 3])
      (?= (seq r1) [3 nil nil])
      (?= (seq r2) [3 nil nil]))))

(deftest reading-and-writing-from-circular-range-ranges
  (let [r1 (srange 3 1 (repeat 5 nil))
        r2 (srange 2 1 [1 2 3 4])]
    (let [[r11 r12] (.ranges r1)]
      (?range= r11 [3 5])
      (?range= r12 [0 1])
      (?range= (r/write! r11 r2) [5 5])
      (?= (seq r1) [3 4 nil])))
  (let [r1 (another-range 3 1 (repeat 5 nil))
        r2 (another-range 2 1 [1 2 3 4])]
    (let [[r11 r12] (.ranges (.range r1))]
      (?range= (.range (r/read! r2 r11)) [4 1])
      (?= (seq (.range r1)) [3 4 nil]))))

(defn- link! [range prev next]
  (LinkedRange. range prev next))

(deftest make-linked-range
  (let [r1 (irange 0 10)
        r2 (irange 10 15)
        r3 (irange -7 0)
        l1 (link! r1 r3 r2)
        l2 (link! r2 r1 nil)
        l3 (link! r3 nil r1)]
    (?= (r/size l1) 10)
    (?= (r/size l2) 5)
    (?= (r/size l3) 7)
    (?true (isa? (type l1) ProxyRange))))

(deftest linked-range-cloning
  (let [r1 (irange 0 10)
        r2 (irange 10 15)
        r3 (irange 10 15)
        lr (link! r1 r3 r2)
        lc (.clone lr)]
    (r/drop! 5 r1)
    (?range= (.range lc) [0 10])
    (r/drop! 5 r2)
    (?range= (.next lc) [15 15])
    (r/drop! 3 r3)
    (?range= (.prev lc) [13 15])))

(deftest linked-range-operations
  (let [r1 (irange 0 10)
        r2 (irange 10 20)
        r3 (irange -10 0)
        lr (link! r1 r3 r2)]
    (r/drop! 3 lr)
    (?range= r3 [-10 3])
    (r/expand! 7 lr)
    (?range= r2 [17 20])))

;concrete range's (nio and arrays) - separate namespaces
