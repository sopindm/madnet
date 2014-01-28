(ns madnet.sequence-test
  (:require [khazad-dum.core :refer :all]
            [madnet.sequence :as r])
  (:import [madnet.sequence Range CircularRange ProxyRange LinkedRange]))

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

(deftest range-equality
  (?= (irange 5 10) (irange 5 10))
  (?= (hash (irange 5 10)) (hash (irange 5 10))))

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
    (?= (.limit cr) r)
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

(deftest circular-range-equality-and-hash
  (let [cr1 (crange 1 2 (irange 0 4))
        cr2 (crange 1 2 (irange 0 4))
        cr3 (crange 1 2 (.limit cr1))
        cr4 (crange 1 2 (irange 0 5))]
    (?= cr1 cr2)
    (?= cr1 cr3)
    (?false (= cr1 cr4))
    (?= (hash cr1) (hash cr2))
    (?= (hash cr1) (hash cr3))
    (?false (= (hash cr1) (hash cr4)))))

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
    (?= (seq (.ranges cr1)) [(crange 0 10 (irange -5 15))])
    (?= (seq (.ranges cr2)) [(crange 8 10 (irange 0 10))
                             (crange 0 3 (irange 0 10))])))

(deftest range-proxy-making
  (let [r (irange 0 10)
        p (r/proxy r)]
    (?= (r/size p) 10)
    (?range= (.range p) [0 10])))
    
(deftest range-proxy-equality-and-hash-code
  (let [r1 (irange 0 10)
        r2 (irange 0 10)
        r3 (irange 1 10)]
    (?= (r/proxy r1) (r/proxy r2))
    (?false (= r1 r3))
    (?= (hash (r/proxy r1)) (hash (r/proxy r2)))
    (?false (= (hash r1) (hash r3)))))

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
    (?= (r/expand 1000 p) p)))

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

(deftest linked-range-equality
  (let [r1 (irange 0 10)
        r2 (irange 10 15)
        r3 (irange -8 0)
        lr (link! r1 r2 r3)]
    (?= lr (link! (.clone r1) r2 r3))
    (?false (= lr (link! (.clone r1) (.clone r2) r3)))
    (?false (= lr (link! (.clone r1) r2 (.clone r3))))
    (?= (hash lr) (hash (link! (.clone r1) r2 r3)))
    (?false (= (hash lr) (hash (link! (.clone r1) (.clone r2) r3))))))

;linked ranges operations
;;take, drop, takeLast, dropLast, expand

;separate range namespace

;concrete range's (nio and arrays) - separate namespaces

;sequence test (rest in sequence namespace)

(comment
  (defmacro ?sequence= [form [position size]]
    `(do (?= (r/position ~form) ~position)
         (?= (r/size ~form) ~size)))

  (declare buffer)

  (defn- test-sequence [buf pos size limit]
    (let [buffer-seq (seq buf)]
      (proxy [ASequence clojure.lang.Seqable] [buf pos size limit]
        (seq [] (clojure.core/seq (take size (drop pos buffer-seq))))
        (writeImpl [ts]
          (if (isa? (type ts) clojure.lang.Seqable)
            (let [write-size (min (r/free-space this) (r/size ts))
                  tseq (take write-size (seq ts))]
              (Pair. (-> (buffer (r/capacity this)
                                 (concat (take (+ pos size) buffer-seq)
                                         tseq
                                         (drop (+ pos size write-size)
                                               buffer-seq)))
                         (.sequence pos size (count buffer-seq))
                         (.expand write-size))
                     (r/drop write-size ts))))))))

  (defn- buffer [size & [content]]
    (let [content (concat content (repeat (- size (count content)) nil))]
      (reify IBuffer
        (size [this] size)
        (sequence [this pos size limit] (test-sequence this pos size limit))
        Object
        (equals [this obj] (and (isa? (class obj) (class this))
                                (= (seq this) (seq obj))))
        clojure.lang.Seqable
        (seq [this] content))))

  (defn- a-sequence [buffer-size position size & [content]]
    (.sequence (buffer buffer-size content) position size buffer-size))

  (deftest making-sequence
    (?sequence= (ASequence. nil 0 100 100) [0 100])
    (?sequence= (a-sequence 100 0 100) [0 100])
    (?= (r/buffer (a-sequence 100 0 100)) (buffer 100)))

  (deftest sequence-capacity-and-free-space
    (let [s (a-sequence 200 0 100)]
      (?= (r/capacity s) 200)
      (?= (r/free-space s) 100)
      (?= (r/capacity (.limit s 150)) 150))
    (let [s2 (a-sequence 200 100 50)]
      (?= (r/free-space s2) 50)))
  
  (deftest sequence-limiting-exceptions
    (let [s (a-sequence 200 100 50)]
      (?throws (.limit s 300) IllegalArgumentException "Limit too much")
      (?throws (.limit s 149) IllegalArgumentException "Limit too low")))

  (deftest sequence-modifiers
    (let [s (a-sequence 200 0 100)]
      (?sequence= (r/take 50 s) [0 50])
      (?= (r/buffer (r/take 50 s)) (r/buffer s))
      (?sequence= (r/drop 50 s) [50 50])
      (?= (r/buffer (r/drop 50 s)) (r/buffer s))
      (?sequence= (r/expand 100 s) [0 200])
      (?= (r/buffer (r/expand 100 s)) (r/buffer s))))

  (deftest sequence-limits-with-take-drop-and-expand
    (let [s (.limit (a-sequence 200 0 100) 190)]
      (?= (r/capacity (r/take 10 s)) 190)
      (?= (r/capacity (r/drop 10 s)) 180)
      (?= (r/capacity (r/expand 10 s)) 190)))

  (deftest take-drop-and-expand-exceptions
    (let [s (.limit (a-sequence 500 0 100) 200)]
      (?throws (r/take -1 s) IllegalArgumentException)
      (?throws (r/take 150 s) BufferUnderflowException)
      (?throws (r/drop -1 s) IllegalArgumentException)
      (?throws (r/drop 150 s) BufferUnderflowException)
      (?throws (r/expand -1 s) IllegalArgumentException)
      (?throws (r/expand 150 s) BufferOverflowException)))

  (deftest sequence-constructor
    (let [s1 (r/sequence (buffer 200))
          s2 (r/sequence (buffer 200) 100)
          s3 (r/sequence (buffer 200) 50 100)]
      (?sequence= s1 [0 0])
      (?sequence= s2 [0 100])
      (?sequence= s3 [50 100])))

  (deftest sequence-with-too-much-size-or-position
    (?throws (r/sequence (buffer 200) 0 500) BufferOverflowException)
    (?throws (r/sequence (buffer 200) 150 100) BufferOverflowException))

  (deftest take-last-and-drop-last-functions
    (let [s (r/sequence (buffer 200) 100)]
      (?sequence= (r/take-last 30 s) [70 30])
      (?sequence= (r/drop-last 30 s) [0 70])
      (?throws (r/drop-last 150 s) BufferUnderflowException)
      (?throws (r/drop-last -1 s) IllegalArgumentException)
      (?throws (r/take-last 150 s) BufferUnderflowException)
      (?throws (r/take-last -1 s) IllegalArgumentException)))

  (deftest split-sequence
    (let [s (r/sequence (buffer 200) 150)
          ss (r/split 80 s)]
      (?= (count ss) 2)
      (?sequence= (first ss) [0 80])
      (?sequence= (second ss) [80 70])))

  (deftest append-sequence
    (let [s (r/sequence (buffer 200) 120)
          ss (r/append 40 s)]
      (?= (count ss) 2)
      (?sequence= (first ss) [120 40])
      (?sequence= (second ss) [0 160])))

  (deftest write-and-read-defaults
    (?throws (r/write (r/sequence (buffer 200 [])) (ASequence. nil 0 0 0))
             UnsupportedOperationException)
    (?throws (r/write (ASequence. nil 0 0 0) (r/sequence (buffer 200 [])))
             UnsupportedOperationException)
    (?throws (r/read (r/sequence (buffer 200 [])) (ASequence. nil 0 0 0))
             UnsupportedOperationException)
    (?throws (r/read (ASequence. nil 0 0 0) (r/sequence (buffer 200 [])))
             UnsupportedOperationException)
    (let [s (r/sequence (buffer 200 []))
          [sw writen] (r/write s (a-sequence 3 0 3 [1 2 3]))
          [sr read] (r/read sw (a-sequence 5 0 2 [1 2]))]
      (?sequence= sw [0 3])
      (?sequence= writen [3 0])
      (?sequence= sr [3 0])
      (?sequence= read [0 5])
      (?= (seq sw) [1 2 3])
      (?= (seq writen) (seq []))
      (?= (seq sr) (seq []))
      (?= (seq read) [1 2 1 2 3])))

  (deftest reading-and-writing-multiple-items
    (let [s (r/sequence (buffer 10 [1 2 3 4 5]) 2 1)
          [sw sws] (r/write s [(r/sequence (buffer 2 [6 7]) 2)
                               (r/sequence (buffer 3 [8 9 10]) 3)
                               (r/sequence (buffer 10) 10)])]
      (?sequence= sw [2 8])
      (?= (seq sw) [3 6 7 8 9 10 nil nil])
      (?= (seq (map seq sws)) [nil nil [nil nil nil nil nil nil nil nil]])
      (let [[sr srs] (r/read sw [(r/sequence (buffer 2))
                                 (r/sequence (buffer 3))
                                 (r/sequence (buffer 1))
                                 (r/sequence (buffer 10))])]
        (?sequence= sr [10 0])
        (?= (seq sr) nil)
        (?= (seq (map seq srs)) [[3 6] [7 8 9] [10] [nil nil]]))))
  
  (defn wrong-multiwriting-seq [seq pos size]
    (letfn [(wsequence [buffer pos size limit]
              (proxy [ASequence] [buffer pos size limit]
                (writeImpl [seq] (Pair. (r/expand 1 this) (r/drop 1 seq)))))]
      (.sequence (reify IBuffer
                   (size [this] (count seq))
                   (sequence [this pos size limit] (wsequence this pos size limit)))
                 pos size (count seq))))

  (deftest multiread-and-multiwrite-errors 
    (let [s (wrong-multiwriting-seq [1 2 3 4 5] 1 1)
          [sw sws] (r/write s (map #(wrong-multiwriting-seq % 0 2) [[1 2] [3 4]]))]
      (?sequence= sw [1 2])
      (?sequence= (first sws) [1 1])
      (?sequence= (second sws) [0 2]))
    (let [s (wrong-multiwriting-seq [1 2 3 4 5] 2 3)
          [sr srs] (r/read s (map #(wrong-multiwriting-seq % 0 0) [[1 2] [3 4]]))]
      (?sequence= sr [3 2])
      (?sequence= (first srs) [0 1])
      (?sequence= (second srs) [0 0])))

  (deftest circular-sequence-metrics
    (let [s (r/sequence (buffer 5) 2 2)
          cs (r/circular-sequence s)]
      (?= (r/buffer cs) (r/buffer s))
      (?= (r/size cs) 2)
      (?= (r/free-space cs) 3)
      (?= (r/capacity cs) 5)))

  (deftest circular-sequence-with-null-buffer
    (?throws (r/circular-sequence (ASequence. nil 0 100 100)) IllegalArgumentException 
             "Sequence must have buffer"))

  (deftest circular-sequence-take-drop-and-expand
    (let [s (r/circular-sequence (r/sequence (buffer 5) 2 2))
          s2 (r/expand 2 s)]
      (?true (isa? (type (r/take 1 s)) CircularSequence))
      (?true (isa? (type (r/drop 3 s2)) CircularSequence))
      (?= (r/size s2) 4)
      (?= (r/free-space s2) 1)
      (?= (r/size (r/take 3 s2)) 3)
      (?= (r/free-space (r/take 3 s2)) 2)
      (?throws (r/take 4 (r/take 3 s2)) BufferUnderflowException)
      (?throws (r/drop 4 (r/take 3 s2)) BufferUnderflowException)
      (?= (r/size (r/drop 3 s2)) 1)
      (?= (r/free-space (r/drop 3 s2)) 4)))

  (deftest circular-sequence-sequencies
    (let [s (r/circular-sequence (r/sequence (buffer 5) 2 2))
          ss (.sequencies s)]
      (?= (count ss) 1)
      (?sequence= (first ss) [2 2])
      (let [s2 (r/expand 2 s)
            ss (.sequencies s2)]
        (?= (count ss) 2)
        (?sequence= (first ss) [2 3])
        (?sequence= (second ss) [0 1])
        (let [ss1 (.sequencies (r/drop 3 s2))
              ss2 (.sequencies (r/take 3 s2))]
          (?= (count ss1) 1)
          (?sequence= (first ss1) [0 1])
          (?= (count ss2) 1)
          (?sequence= (first ss2) [2 3])))))

  (deftest circular-sequencies-read
    (let [s (r/circular-sequence (r/sequence (buffer 5 [1 2 3 4 5]) 2 2))
          s2 (r/expand 2 s)
          s3 (r/expand 3 s)]
      (let [[read sr] (r/read s (r/sequence (buffer 5) 0 0))]
        (?sequence= sr [0 2])
        (?= (seq sr) [3 4])
        (?= (r/size read) 0)
        (?= (r/free-space read) 5))
      (let [[read sr] (r/read s2 (r/sequence (buffer 5) 0 0))]
        (?sequence= sr [0 4])
        (?= (seq sr) [3 4 5 1])
        (?= (r/size read) 0)
        (?= (r/free-space read) 5))
      (let [[read sr] (r/read s3 (r/sequence (buffer 4) 0 0))
            [_ sr] (r/read read (r/sequence (buffer 5) 0 0))]
        (?= (seq sr) [2]))))

  (deftest circular-sequencies-write
    (let [s (r/circular-sequence (r/sequence (buffer 5 [1 2 3 4 5]) 2 0))
          s2 (r/expand 2 s)]
      (let [[writen sw] (r/write s (r/sequence (buffer 2 [6 7]) 0 2))]
        (?sequence= sw [2 0])
        (?= (r/size writen) 2)
        (?= (r/free-space writen) 3)
        (?= (seq (mapcat seq (.sequencies writen))) [6 7]))
      (let [[writen sw] (r/write s2 (r/sequence (buffer 2 [6 7]) 0 2))]
        (?sequence= sw [2 0])
        (?= (r/size writen) 4)
        (?= (r/free-space writen) 1)
        (?= (seq (mapcat seq (.sequencies writen))) [3 4 6 7]))
      (let [[writen sw] (r/write s2 (r/sequence (buffer 5 (range 5)) 0 5))]
        (?sequence= sw [3 2])
        (?= (seq (mapcat seq (.sequencies writen))) [3 4 0 1 2]))
      (let [[writen sw] (r/write (r/expand 2 s2)
                                 (r/sequence (buffer 5 (range 5)) 0 5))]
        (?sequence= sw [1 4])
        (?= (seq (mapcat seq (.sequencies writen))) [3 4 5 1 0])))))

;mutable sequence functions
;sequencies access (random or sequential)

;clojure collections as buffers
;clojure collections as sequencies


