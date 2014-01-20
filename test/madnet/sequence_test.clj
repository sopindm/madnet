(ns madnet.sequence-test
  (:require [khazad-dum.core :refer :all]
            [madnet.sequence :as s])
  (:import [madnet.sequence Range CircularRange]))

(defmacro ?range= [expr [begin end]]
  `(let [range# ~expr]
     (?= (.begin range#) ~begin)
     (?= (.end range#) ~end)))

(defmacro range-proxy [[& class-and-interfaces] [min-arg max-arg & constructor-args] & methods]
  (let [arg-sym 'n this-sym 'this]
    `(let [~min-arg (atom (int ~min-arg))
           ~max-arg (atom (int ~max-arg))]
       (proxy [~@class-and-interfaces] [@~min-arg @~max-arg ~@constructor-args]
         (begin
           ([] @~min-arg)
           ([~arg-sym] (reset! ~min-arg ~arg-sym) ~this-sym))
         (end 
           ([] @~max-arg)
           ([~arg-sym] (reset! ~max-arg ~arg-sym) ~this-sym))
         ~@methods))))

(defn- irange [min max]
  (range-proxy [Range] [min max] 
    (clone [] (irange @min @max))))

(deftest making-range
  (?range= (irange 5 15) [5 15])
  (?range= (irange 5 5) [5 5])
  (?throws (irange 6 5) IllegalArgumentException)
  (?= (s/size (irange 5 10)) 5))

(deftest range-equality
  (?= (irange 5 10) (irange 5 10))
  (?= (hash (irange 5 10)) (hash (irange 5 10))))

(deftest range-mutable-take-drop-and-expand
  (let [r (irange 5 10)]
    (?range= (s/take! 3 r) [5 8])
    (?range= r [5 8])
    (?range= (s/drop! 1 r) [6 8])
    (?range= r [6 8])
    (?range= (s/expand! 5 r) [6 13])
    (?range= r [6 13])
    (?throws (s/take! 10 r) IndexOutOfBoundsException)
    (?throws (s/drop! 10 r) IndexOutOfBoundsException)
    (?range= (s/drop-last! 2 r) [6 11])
    (?range= (s/take-last! 3 r) [8 11])
    (?throws (s/take-last! 10 r) IndexOutOfBoundsException)
    (?throws (s/drop-last! 10 r) IndexOutOfBoundsException)))

(deftest range-cloning
  (let [r (irange 5 10)
        c (.clone r)]
    (s/drop! 1 r)
    (?range= r [6 10])
    (?range= c [5 10])))

(deftest mutable-range-split
  (let [r (irange 5 10)
        s (s/split! 3 r)]
    (?range= r [8 10])
    (?range= s [5 8])))

(deftest immutable-range-operations
  (let [r (irange 5 10)]
    (?range= (s/take 3 r) [5 8])
    (?range= r [5 10])
    (?range= (s/take-last 3 r) [7 10])
    (?range= r [5 10])
    (?range= (s/drop 2 r) [7 10])
    (?range= r [5 10])
    (?range= (s/drop-last 2 r) [5 8])
    (?range= r [5 10])
    (?range= (s/expand 5 r) [5 15])
    (?range= r [5 10])
    (let [[spliten rest] (s/split 3 r)]
      (?range= spliten [5 8])
      (?range= rest [8 10])
      (?range= r [5 10]))))

(defn- crange [min max limit]
  (range-proxy [CircularRange] [min max limit]
    (clone [] (crange @min @max limit))))

(deftest making-circular-ranges
  (let [r (irange 5 15)
        cr (crange 10 6 r)]
    (?range= cr [10 6])
    (?= (.limit cr) r)
    (s/take! 5 r)
    (?range= (.limit cr) [5 15])
    (s/take! 5 (.limit cr))
    (?range= (.limit cr) [5 15])
    (?throws (crange 5 10 (irange 6 10) IllegalArgumentException))
    (?throws (crange 5 10 (irange 5 9) IllegalArgumentException))))

;exception creating circular range not in limit

(deftest circular-range-cloning
  (let [cr1 (crange 1 2 (irange 0 4))
        l1 (.limit cr1)
        cr2 (.clone cr1)]
    (?range= cr2 [1 2])
    (?range= (.limit cr2) [0 4])
    (s/take! 1 (.limit cr1))
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

(deftest ciruclar-range-operations
  (let [cr (crange 0 0 (irange -5 5))]
    (?range= (s/expand! 7 cr) [0 -3])
    (?range= (s/take 6 cr) [0 -4])
    (?range= (s/drop 6 cr) [-4 -3])
    (?range= (s/take-last 1 cr) [-4 -3])
    (?range= (s/drop-last 2 cr) [0 -5])))

(comment
  (defmacro ?sequence= [form [position size]]
    `(do (?= (s/position ~form) ~position)
         (?= (s/size ~form) ~size)))

  (declare buffer)

  (defn- test-sequence [buf pos size limit]
    (let [buffer-seq (seq buf)]
      (proxy [ASequence clojure.lang.Seqable] [buf pos size limit]
        (seq [] (clojure.core/seq (take size (drop pos buffer-seq))))
        (writeImpl [ts]
          (if (isa? (type ts) clojure.lang.Seqable)
            (let [write-size (min (s/free-space this) (s/size ts))
                  tseq (take write-size (seq ts))]
              (Pair. (-> (buffer (s/capacity this)
                                 (concat (take (+ pos size) buffer-seq)
                                         tseq
                                         (drop (+ pos size write-size)
                                               buffer-seq)))
                         (.sequence pos size (count buffer-seq))
                         (.expand write-size))
                     (s/drop write-size ts))))))))

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
    (?= (s/buffer (a-sequence 100 0 100)) (buffer 100)))

  (deftest sequence-capacity-and-free-space
    (let [s (a-sequence 200 0 100)]
      (?= (s/capacity s) 200)
      (?= (s/free-space s) 100)
      (?= (s/capacity (.limit s 150)) 150))
    (let [s2 (a-sequence 200 100 50)]
      (?= (s/free-space s2) 50)))
  
  (deftest sequence-limiting-exceptions
    (let [s (a-sequence 200 100 50)]
      (?throws (.limit s 300) IllegalArgumentException "Limit too much")
      (?throws (.limit s 149) IllegalArgumentException "Limit too low")))

  (deftest sequence-modifiers
    (let [s (a-sequence 200 0 100)]
      (?sequence= (s/take 50 s) [0 50])
      (?= (s/buffer (s/take 50 s)) (s/buffer s))
      (?sequence= (s/drop 50 s) [50 50])
      (?= (s/buffer (s/drop 50 s)) (s/buffer s))
      (?sequence= (s/expand 100 s) [0 200])
      (?= (s/buffer (s/expand 100 s)) (s/buffer s))))

  (deftest sequence-limits-with-take-drop-and-expand
    (let [s (.limit (a-sequence 200 0 100) 190)]
      (?= (s/capacity (s/take 10 s)) 190)
      (?= (s/capacity (s/drop 10 s)) 180)
      (?= (s/capacity (s/expand 10 s)) 190)))

  (deftest take-drop-and-expand-exceptions
    (let [s (.limit (a-sequence 500 0 100) 200)]
      (?throws (s/take -1 s) IllegalArgumentException)
      (?throws (s/take 150 s) BufferUnderflowException)
      (?throws (s/drop -1 s) IllegalArgumentException)
      (?throws (s/drop 150 s) BufferUnderflowException)
      (?throws (s/expand -1 s) IllegalArgumentException)
      (?throws (s/expand 150 s) BufferOverflowException)))

  (deftest sequence-constructor
    (let [s1 (s/sequence (buffer 200))
          s2 (s/sequence (buffer 200) 100)
          s3 (s/sequence (buffer 200) 50 100)]
      (?sequence= s1 [0 0])
      (?sequence= s2 [0 100])
      (?sequence= s3 [50 100])))

  (deftest sequence-with-too-much-size-or-position
    (?throws (s/sequence (buffer 200) 0 500) BufferOverflowException)
    (?throws (s/sequence (buffer 200) 150 100) BufferOverflowException))

  (deftest take-last-and-drop-last-functions
    (let [s (s/sequence (buffer 200) 100)]
      (?sequence= (s/take-last 30 s) [70 30])
      (?sequence= (s/drop-last 30 s) [0 70])
      (?throws (s/drop-last 150 s) BufferUnderflowException)
      (?throws (s/drop-last -1 s) IllegalArgumentException)
      (?throws (s/take-last 150 s) BufferUnderflowException)
      (?throws (s/take-last -1 s) IllegalArgumentException)))

  (deftest split-sequence
    (let [s (s/sequence (buffer 200) 150)
          ss (s/split 80 s)]
      (?= (count ss) 2)
      (?sequence= (first ss) [0 80])
      (?sequence= (second ss) [80 70])))

  (deftest append-sequence
    (let [s (s/sequence (buffer 200) 120)
          ss (s/append 40 s)]
      (?= (count ss) 2)
      (?sequence= (first ss) [120 40])
      (?sequence= (second ss) [0 160])))

  (deftest write-and-read-defaults
    (?throws (s/write (s/sequence (buffer 200 [])) (ASequence. nil 0 0 0))
             UnsupportedOperationException)
    (?throws (s/write (ASequence. nil 0 0 0) (s/sequence (buffer 200 [])))
             UnsupportedOperationException)
    (?throws (s/read (s/sequence (buffer 200 [])) (ASequence. nil 0 0 0))
             UnsupportedOperationException)
    (?throws (s/read (ASequence. nil 0 0 0) (s/sequence (buffer 200 [])))
             UnsupportedOperationException)
    (let [s (s/sequence (buffer 200 []))
          [sw writen] (s/write s (a-sequence 3 0 3 [1 2 3]))
          [sr read] (s/read sw (a-sequence 5 0 2 [1 2]))]
      (?sequence= sw [0 3])
      (?sequence= writen [3 0])
      (?sequence= sr [3 0])
      (?sequence= read [0 5])
      (?= (seq sw) [1 2 3])
      (?= (seq writen) (seq []))
      (?= (seq sr) (seq []))
      (?= (seq read) [1 2 1 2 3])))

  (deftest reading-and-writing-multiple-items
    (let [s (s/sequence (buffer 10 [1 2 3 4 5]) 2 1)
          [sw sws] (s/write s [(s/sequence (buffer 2 [6 7]) 2)
                               (s/sequence (buffer 3 [8 9 10]) 3)
                               (s/sequence (buffer 10) 10)])]
      (?sequence= sw [2 8])
      (?= (seq sw) [3 6 7 8 9 10 nil nil])
      (?= (seq (map seq sws)) [nil nil [nil nil nil nil nil nil nil nil]])
      (let [[sr srs] (s/read sw [(s/sequence (buffer 2))
                                 (s/sequence (buffer 3))
                                 (s/sequence (buffer 1))
                                 (s/sequence (buffer 10))])]
        (?sequence= sr [10 0])
        (?= (seq sr) nil)
        (?= (seq (map seq srs)) [[3 6] [7 8 9] [10] [nil nil]]))))
  
  (defn wrong-multiwriting-seq [seq pos size]
    (letfn [(wsequence [buffer pos size limit]
              (proxy [ASequence] [buffer pos size limit]
                (writeImpl [seq] (Pair. (s/expand 1 this) (s/drop 1 seq)))))]
      (.sequence (reify IBuffer
                   (size [this] (count seq))
                   (sequence [this pos size limit] (wsequence this pos size limit)))
                 pos size (count seq))))

  (deftest multiread-and-multiwrite-errors 
    (let [s (wrong-multiwriting-seq [1 2 3 4 5] 1 1)
          [sw sws] (s/write s (map #(wrong-multiwriting-seq % 0 2) [[1 2] [3 4]]))]
      (?sequence= sw [1 2])
      (?sequence= (first sws) [1 1])
      (?sequence= (second sws) [0 2]))
    (let [s (wrong-multiwriting-seq [1 2 3 4 5] 2 3)
          [sr srs] (s/read s (map #(wrong-multiwriting-seq % 0 0) [[1 2] [3 4]]))]
      (?sequence= sr [3 2])
      (?sequence= (first srs) [0 1])
      (?sequence= (second srs) [0 0])))

  (deftest circular-sequence-metrics
    (let [s (s/sequence (buffer 5) 2 2)
          cs (s/circular-sequence s)]
      (?= (s/buffer cs) (s/buffer s))
      (?= (s/size cs) 2)
      (?= (s/free-space cs) 3)
      (?= (s/capacity cs) 5)))

  (deftest circular-sequence-with-null-buffer
    (?throws (s/circular-sequence (ASequence. nil 0 100 100)) IllegalArgumentException 
             "Sequence must have buffer"))

  (deftest circular-sequence-take-drop-and-expand
    (let [s (s/circular-sequence (s/sequence (buffer 5) 2 2))
          s2 (s/expand 2 s)]
      (?true (isa? (type (s/take 1 s)) CircularSequence))
      (?true (isa? (type (s/drop 3 s2)) CircularSequence))
      (?= (s/size s2) 4)
      (?= (s/free-space s2) 1)
      (?= (s/size (s/take 3 s2)) 3)
      (?= (s/free-space (s/take 3 s2)) 2)
      (?throws (s/take 4 (s/take 3 s2)) BufferUnderflowException)
      (?throws (s/drop 4 (s/take 3 s2)) BufferUnderflowException)
      (?= (s/size (s/drop 3 s2)) 1)
      (?= (s/free-space (s/drop 3 s2)) 4)))

  (deftest circular-sequence-sequencies
    (let [s (s/circular-sequence (s/sequence (buffer 5) 2 2))
          ss (.sequencies s)]
      (?= (count ss) 1)
      (?sequence= (first ss) [2 2])
      (let [s2 (s/expand 2 s)
            ss (.sequencies s2)]
        (?= (count ss) 2)
        (?sequence= (first ss) [2 3])
        (?sequence= (second ss) [0 1])
        (let [ss1 (.sequencies (s/drop 3 s2))
              ss2 (.sequencies (s/take 3 s2))]
          (?= (count ss1) 1)
          (?sequence= (first ss1) [0 1])
          (?= (count ss2) 1)
          (?sequence= (first ss2) [2 3])))))

  (deftest circular-sequencies-read
    (let [s (s/circular-sequence (s/sequence (buffer 5 [1 2 3 4 5]) 2 2))
          s2 (s/expand 2 s)
          s3 (s/expand 3 s)]
      (let [[read sr] (s/read s (s/sequence (buffer 5) 0 0))]
        (?sequence= sr [0 2])
        (?= (seq sr) [3 4])
        (?= (s/size read) 0)
        (?= (s/free-space read) 5))
      (let [[read sr] (s/read s2 (s/sequence (buffer 5) 0 0))]
        (?sequence= sr [0 4])
        (?= (seq sr) [3 4 5 1])
        (?= (s/size read) 0)
        (?= (s/free-space read) 5))
      (let [[read sr] (s/read s3 (s/sequence (buffer 4) 0 0))
            [_ sr] (s/read read (s/sequence (buffer 5) 0 0))]
        (?= (seq sr) [2]))))

  (deftest circular-sequencies-write
    (let [s (s/circular-sequence (s/sequence (buffer 5 [1 2 3 4 5]) 2 0))
          s2 (s/expand 2 s)]
      (let [[writen sw] (s/write s (s/sequence (buffer 2 [6 7]) 0 2))]
        (?sequence= sw [2 0])
        (?= (s/size writen) 2)
        (?= (s/free-space writen) 3)
        (?= (seq (mapcat seq (.sequencies writen))) [6 7]))
      (let [[writen sw] (s/write s2 (s/sequence (buffer 2 [6 7]) 0 2))]
        (?sequence= sw [2 0])
        (?= (s/size writen) 4)
        (?= (s/free-space writen) 1)
        (?= (seq (mapcat seq (.sequencies writen))) [3 4 6 7]))
      (let [[writen sw] (s/write s2 (s/sequence (buffer 5 (range 5)) 0 5))]
        (?sequence= sw [3 2])
        (?= (seq (mapcat seq (.sequencies writen))) [3 4 0 1 2]))
      (let [[writen sw] (s/write (s/expand 2 s2)
                                 (s/sequence (buffer 5 (range 5)) 0 5))]
        (?sequence= sw [1 4])
        (?= (seq (mapcat seq (.sequencies writen))) [3 4 5 1 0])))))

;mutable sequence functions
;sequencies access (random or sequential)

;clojure collections as buffers
;clojure collections as sequencies


