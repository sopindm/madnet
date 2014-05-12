(ns madnet.sequence-test
  (:require [khazad-dum :refer :all]
            [madnet.channel-test :refer [?unsupported]]
            [madnet.channel :as c] 
            [madnet.sequence :as s])
  (:import [madnet.channel Result]
           [madnet.sequence Sequence ReadableSequence WritableSequence]))

;;
;; Sequence
;;

(deftest sequencies-have-begin-end-size-and-free-space
  (let [s (Sequence.)]
    (?unsupported (s/begin s))
    (?unsupported (s/end s))
    (?unsupported (s/size s))
    (?unsupported (s/free-space s))))

(defn a-sequence ([begin size] (a-sequence begin size (Integer/MAX_VALUE)))
  ([begin size space]
     (let [begin (atom begin)
           size (atom size)]
       (proxy [Sequence] []
         (begin [] @begin)
         (begin_$eq [n] (reset! begin n))
         (size [] @size)
         (size_$eq [n] (reset! size n))
         (freeSpace [] (- space @size @begin))))))

(deftest sequence-end-implemented-using-begin-and-size
  (?= (s/end (a-sequence 10 100)) 110))

(deftest sequencies-have-take-drop-and-expand-methods
  (let [s (Sequence.)]
    (?unsupported (s/take! 10 s))
    (?unsupported (s/drop! 10 s))
    (?unsupported (s/expand! 10 s))))

(defmacro ?sequence=
  ([expr begin size]
     `(let [value# ~expr]
        (?= (s/begin value#) ~begin)
        (?= (s/size value#) ~size)))
  ([expr begin size free-space]
     `(let [value# ~expr]
        (?= (s/begin value#) ~begin)
        (?= (s/size value#) ~size)
        (?= (s/free-space value#) ~free-space))))

(deftest sequence-take-drop-and-expand-methods-are-implemented-using-begin-and-end-setters
  (let [s (a-sequence 10 90 100)]
    (?sequence= (s/take! 10 s) 10 10 80)
    (?sequence= (s/drop! 3 s) 13 7 80)
    (?sequence= (s/expand! 20 s) 13 27 60)))

(deftest sequence-throws-bufferoverflowexception-on-trying-to-expand-too-much
  (let [s (a-sequence 0 10 100)]
    (?throws (s/expand! 91 s) java.nio.BufferOverflowException)))

(deftest sequence-throws-bufferunderflowexception-on-trying-to-take-or-drop-too-much
  (let [s (a-sequence 0 10 100)]
    (?throws (s/take! 11 s) java.nio.BufferUnderflowException)
    (?throws (s/drop! 11 s) java.nio.BufferUnderflowException)))

;;
;; Readable sequence
;;

(deftest readable-sequence-has-random-get-access
  (?unsupported (s/get (ReadableSequence.) 10)))

(defn readable-sequence
  ([seq] (readable-sequence seq (count seq)))
  ([seq size] (readable-sequence seq 0 size))
  ([seq begin size]
     (let [begin (atom begin) size (atom size)]
       (proxy [ReadableSequence] []
         (size [] @size)
         (drop [n] (swap! begin + n) (swap! size - n))
         (get [n] (nth seq (+ @begin n)))))))

(deftest readable-sequence-default-pop-implementation-uses-get-and-drop
  (let [s (readable-sequence (range 2))]
    (?= (c/pop! s) 0)
    (?= (c/pop! s) 1)
    (?= (c/try-pop! s) nil)))

(deftest readable-sequencies-implements-java-iterable
  (let [s (readable-sequence (range 10))]
    (?= (seq s) (seq (range 10)))))

;;
;; Writable sequence
;;

(deftest writable-sequencies-have-random-set-access
  (let [s (WritableSequence.)]
    (?unsupported (s/set! s 42 100500))))

(defn writable-sequence
  ([seq] (writable-sequence seq (count seq)))
  ([seq size] (writable-sequence seq 0 size))
  ([seq begin size]
     (let [seq (atom seq) begin (atom begin) size (atom size)]
       (proxy [WritableSequence Iterable] []
         (size [] @size)
         (freeSpace [] (- (count @seq) @begin @size))
         (expand [n] (swap! size + n))
         (set [n value] (let [n (+ n @begin)]
                          (swap! seq #(concat (take n %) [value] (drop (inc n) %)))))
         (iterator [] (.iterator @seq))))))

(deftest writable-sequence-push-implementation-uses-set-and-expand
  (let [s (writable-sequence (repeat 2 nil) 0)]
    (?= (c/try-push! s 100) true)
    (?= (c/try-push! s 500) true)
    (?= (c/try-push! s 100500) false)
    (?= (seq s) [100 500])))

(deftest default-read-implementation-can-write-to-writable-sequence
  (let [reader (readable-sequence (range 5))
        writer (writable-sequence (repeat 10 nil) 2)]
    (?= (c/read! reader writer) (Result. 5))
    (?= (seq reader) nil)
    (?= (seq writer)
        (seq (concat (repeat 2 nil) (range 5) (repeat 3 nil)))))
  (let [reader (readable-sequence (range 10) 2 5)
        writer (writable-sequence (repeat 5 nil) 1 1)]
    (?= (c/read! reader writer) (Result. 3))
    (?= (seq reader) (range 5 7))
    (?= (seq writer) (seq (concat [nil nil] (range 2 5))))))

;buffer (sequence factory)
;sequence factory function

;ranges
;;ranges have size and free space
;;ranges are clojure sequencies
;;ranges can be thread safe
;;ranges can auto-close read on emptiness
;;ranges can auto-close write on fullness

;circular ranges
;;circular ranges reuse beginning of underlying buffer
;;circular ranges can have compaction ratio

;concrete ranges
;;byte ranges (over nio byte buffers)
;;char ranges (over nio char buffers)
;;object ranges
;;;object ranges cannot store nulls
