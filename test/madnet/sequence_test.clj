(ns madnet.sequence-test
  (:require [khazad-dum.core :refer :all]
            [madnet.sequence :as s])
  (:import [madnet.sequence]))

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


