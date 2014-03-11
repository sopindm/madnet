(ns madnet.sequence-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c]
            [madnet.buffer :as b]
            [madnet.range :as r]
            [madnet.sequence :as s]
            [madnet.range-test :refer :all])
  (:import [madnet.range IntegerRange ObjectRange]
           [madnet.channel Result]))

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
    (?range= (s/writer s) [1 8]))
  (let [s (s/sequence [10 :circular true] :reader [5 5])]
    (?range= (s/writer s) [5 5])
    (?= (-> s s/writer r/size) 10))
  (let [s (s/sequence [10 :circular true] :writer [5 5])]
    (?range= (s/reader s) [5 5])
    (?= (-> s s/reader r/size) 10)))

(deftest sequence-seq
  (let [s (s/sequence (b/wrap (range -5 5) {:circular true}) :reader [7 3])]
    (?= (seq s) [2 3 4 -5 -4 -3])))

(deftest reading-from-sequence
  (let [s (s/sequence (b/wrap (range -5 5)) :reader [0 10])
        r ((b/buffer 10))
        rc (.clone r)]
    (?= (c/read! s r) (Result. 10 10))
    (?range= (.reader s) [10 10])
    (?range= r [10 10])
    (?range= (s/reader s) [10 10])
    (?range= (s/writer s) [10 10])
    (?= (seq rc) (seq (range -5 5)))
    (?= (.read s ((b/buffer 10 {:element :byte}))) nil)))

(deftest writing-to-sequence
  (let [s (s/sequence [10 :element :byte :circular false])
        r ((b/wrap (byte-array (map byte (range 10)))))
        rc (.clone r)]
    (c/write! s r)
    (?range= (s/reader s) [0 10])
    (?range= (s/writer s) [10 10])
    (?range= r [10 10])
    (?= (seq s) (seq (map byte (range 10)))))) 

(deftest cloning-sequence
  (let [s (s/sequence 10 :reader [3 8] :writer [5 2])
        sc (.clone s)]
    (?true (identical? (.buffer s) (.buffer sc)))
    (?range= (s/reader sc) [3 8])
    (?range= (s/writer sc) [5 2])
    (r/drop! 2 (s/reader s))
    (?range= (s/reader sc) [3 8])
    (r/drop! 2 (s/writer s))
    (?range= (s/writer sc) [5 2])))

(deftest circular-reading-and-writing
  (let [s (s/sequence 10 :reader [8 8])]
    (c/write! s ((b/wrap (range 8))))
    (?= (seq (take 8 (seq s))) [0 1 2 3 4 5 6 7])
    (?range= (s/reader s) [8 6])
    (?range= (s/writer s) [6 8])
    (let [r ((b/wrap (repeat 4 nil)))
          rc (.clone r)]
      (c/read! s r)
      (?= (seq rc) [0 1 2 3]))
    (?range= (s/reader s) [2 6])
    (?range= (s/writer s) [6 2])))

(deftest wrapped-sequencies
  (let [s (s/wrap (range 100))]
    (?range= (s/reader s) [0 100])
    (?range= (s/writer s) [100 100])
    (?false (s/circular? s))
    (?= (seq s) (seq (range 100)))
    (let [r ((b/buffer 100))
          [rs _] (c/read s r)]
      (?range= (s/reader rs) [100 100])
      (?range= (s/writer rs) [100 100])
      (?= (seq r) (seq (range 100))))))

(deftest writing-sequence-to-sequence
  (let [[s _] (c/write (s/sequence 10) (s/wrap (range -5 15)))]
    (?range= (s/reader s) [0 10])
    (?range= (s/writer s) [0 0])
    (?= (seq s) (seq (range -5 5)))))

(deftest pushing-to-sequence
  (let [s (s/sequence 3)]
    (?= (c/push! s 123) s)
    (?= (c/push! s 234 :timeout 0) s)
    (?= (c/push! s 345 :timeout 10) s)
    (?= (seq s) [123 234 345])))

(deftest pushing-to-full-sequence
  (let [s (s/sequence 1 :reader [0 0] :writer [0 0])
        f (future (c/push! s 123))]
    (Thread/sleep 2)
    (?false (realized? f))
    (.expand (.writer s) 1)
    (?= @f s)))

(deftest pushing-to-full-sequence-with-zero-timeout
  (let [s (s/sequence 1 :writer [0 0])]
    (?= (c/push! s 123 :timeout 0) nil)))

(deftest pushing-to-full-sequence-with-timeout
  (let [s (s/sequence 0)
        f (future (c/push! s 123 :timeout 4))]
    (Thread/sleep 1)
    (?false (realized? f))
    (Thread/sleep 4)
    (?true (realized? f))
    (?= @f nil)))

(deftest popping-from-sequence
  (let [s (s/wrap [1 2 3])]
    (?= (c/pop! s) 1)
    (?= (c/pop! s :timeout 0) 2)
    (?= (c/pop! s :timeout 10) 3)
    (?range= (.writer s) [3 3])))

(deftest popping-from-circular-sequence
  (let [s (s/sequence (b/wrap [1 2 3] {:circular true}) :reader [0 3] :writer [0 0])]
    (?= (c/pop! s) 1)
    (?= (c/pop! s :timeout 0) 2)
    (?= (c/pop! s :timemout 10) 3)
    (?range= (.writer s) [0 3])))

(deftest popping-from-full-sequence-with-zero-timeout
  (let [s (s/sequence 0)]
    (?= (c/pop! s :timeout 0) nil)))

(deftest popping-from-full-sequence
  (let [s (s/sequence (b/wrap [0]) :reader [0 0] :writer [1 1])
        f (future (c/pop! s))]
    (Thread/sleep 2)
    (?false (realized? f))
    (r/expand! 1 (.reader s))
    (?= @f 0)))

(deftest popping-from-full-sequence-with-timeout
  (let [s (s/sequence (b/wrap [0]) :reader [0 0] :writer [1 1])
        f (future (c/pop! s :timeout 5))]
    (Thread/sleep 2)
    (?false (realized? f))
    (Thread/sleep 4)
    (?true (realized? f))
    (?= @f nil)))



    
    


