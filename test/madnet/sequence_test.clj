(ns madnet.sequence-test
  (:require [khazad-dum :refer :all]
            [madnet.channel-test :refer [?unsupported]]
            [madnet.channel :as c] 
            [madnet.sequence :as s])
  (:import [java.nio ByteBuffer CharBuffer]
            [madnet.channel Result]
           [madnet.sequence Sequence Sequence$
                            CircularSequence InputCircularSequence OutputCircularSequence
                            ByteSequence$ CharSequence$
                            InputSequence OutputSequence IOSequence
                            ObjectSequence InputObjectSequence OutputObjectSequence
                            NIOSequence
                            InputByteSequence OutputByteSequence
                            InputCharSequence OutputCharSequence]))

;;
;; Sequence
;;

(deftest sequencies-have-begin-end-size-free-space-and-clone
  (let [s (Sequence.)]
    (?unsupported (s/begin s))
    (?unsupported (s/end s))
    (?unsupported (s/size s))
    (?unsupported (s/free-space s))
    (?unsupported (s/clone s))))

(defn a-sequence ([begin size] (a-sequence begin size (Integer/MAX_VALUE)))
  ([begin size space]
     (let [begin (atom begin)
           size (atom size)]
       (proxy [Sequence] []
         (begin [] @begin)
         (begin_$eq [n] (reset! begin n))
         (size [] @size)
         (size_$eq [n] (reset! size n))
         (freeSpace [] (- space @begin @size))))))

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
  (?unsupported (s/get (InputSequence.) 10)))

(defn readable-sequence
  ([seq] (readable-sequence seq (count seq)))
  ([seq size] (readable-sequence seq 0 size))
  ([seq begin size & [result]]
     (let [begin (atom begin) size (atom size)]
       (proxy [InputSequence] []
         (size [] @size)
         (begin [] @begin)
         (freeSpace [] (- (count seq) @begin @size))
         (drop [n] (swap! begin + n) (swap! size - n))
         (expand [n] (swap! size + n))
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
  (let [s (OutputSequence.)]
    (?unsupported (s/set! s 42 100500))))

(defn writable-sequence
  ([seq] (writable-sequence seq (count seq)))
  ([seq size] (writable-sequence seq 0 size))
  ([seq begin size & [result]]
     (let [seq (atom seq) begin (atom begin) size (atom size)]
       (proxy [OutputSequence Iterable] []
         (begin [] @begin)
         (size [] @size)
         (freeSpace [] (- (count @seq) @begin @size))
         (drop [n] (swap! begin + n) (swap! size - n))
         (expand [n] (swap! size + n))
         (set [n value] (let [n (+ n @begin)]
                          (swap! seq #(concat (take n %) [value] (drop (inc n) %)))))
         (iterator [] (.iterator @seq))))))

(deftest writable-sequence-push-implementation-uses-set-and-expand
  (let [s (writable-sequence (repeat 3 nil) 2)]
    (?= (c/try-push! s 100) true)
    (?= (c/try-push! s 500) true)
    (?= (c/try-push! s 100500) false)
    (?= (seq s) [100 500 nil])))

(deftest writing-from-input-sequence-to-output-sequence
  (let [reader (readable-sequence (range 5))
        writer (writable-sequence (repeat 10 nil) 8)]
    (?= (.write Sequence$/MODULE$ writer reader) (Result. 5))
    (?= (seq reader) nil)
    (?= (seq writer)
        (seq (concat (range 5) (repeat 5 nil)))))
  (let [reader (readable-sequence (range 10) 2 5)
        writer (writable-sequence (repeat 5 nil) 1 2)]
    (?= (.write Sequence$/MODULE$ writer reader) (Result. 2))
    (?= (seq reader) (range 4 7))
    (?= (seq writer) [nil 2 3 nil nil])))

;;
;; IO Sequences
;;

(defn io-sequence [reader writer & {:keys [linked] :or {linked false}}]
  (proxy [IOSequence] [linked]
    (reader [] reader)
    (writer [] writer)))

(deftest io-sequence-have-begin-end-size-and-free-space
  (let [s (io-sequence (readable-sequence (range 4) 1 2)
                       (writable-sequence (range 5) 2 1))]
    (?sequence= s 1 2 2)
    (?sequence= (c/reader s) 1 2 1)
    (?sequence= (c/writer s) 2 1 2)))

(deftest taking-not-supported-for-io-sequencies
  (?unsupported (s/take! 10 (io-sequence (readable-sequence nil) (writable-sequence nil)))))

(deftest dropping-from-io-sequence-drops-from-the-reader
  (let [s (io-sequence (readable-sequence (range 5)) (writable-sequence (range 10)))]
    (s/drop! 3 s)
    (?sequence= s 3 2 0)
    (?sequence= (c/reader s) 3 2 0)))

(deftest expanding-io-sequence-expands-writer
  (let [s (io-sequence nil (writable-sequence (range 5) 3))]
    (s/expand! 2 s)
    (?sequence= (c/writer s) 0 5 0)))

(deftest io-sequence-iterable-over-reader
  (?= (seq (io-sequence (readable-sequence (range 5)) nil)) (seq (range 5))))

(deftest io-sequence-pushes-to-writer-and-expands-reader
  (let [s (io-sequence (readable-sequence (range 5) 3) (writable-sequence (repeat 5 nil)))]
    (?= (c/try-push! s 10) true)
    (?sequence= (c/reader s) 0 4)
    (?sequence= (c/writer s) 1 4)
    (?= (first (seq (c/writer s))) 10)))

(deftest io-sequence-pop-pops-from-reader
  (let [s (io-sequence (readable-sequence (range 3 5)) nil)]
    (?= (c/try-pop! s) 3)
    (?sequence= (c/reader s) 1 1)))

(deftest droping-from-linked-io-sequence
  (let [s (io-sequence (readable-sequence (repeat 10 nil) 1 4)
                       (writable-sequence (repeat 10 nil) 2 7) :linked true)]
    (s/drop! 3 s)
    (?sequence= (c/reader s) 4 1)
    (?sequence= (c/writer s) 2 10)))

(deftest expanding-linked-io-sequence
  (let [s (io-sequence (readable-sequence (repeat 10 nil) 1 7)
                       (writable-sequence (repeat 10 nil) 2 4) :linked true)]
    (s/expand! 3 s)
    (?sequence= (c/reader s) 4 4)
    (?sequence= (c/writer s) 2 7)))

(deftest linked-io-sequence-free-space
  (let [s (io-sequence (readable-sequence (repeat 10 nil) 2 2)
                       (writable-sequence (repeat 10 nil) 4 0) :linked true)]
    (?= (s/free-space s) 2)))
    

;;
;; Object sequence
;;

(deftest object-sequence-begin-size-and-free-space
  (?sequence= (ObjectSequence. (object-array 100) 15 62) 15 62 23)
  (?throws (ObjectSequence. (object-array 10) 5 10) IllegalArgumentException))

(deftest object-sequence-taking-and-droping
  (?sequence= (s/take! 15 (ObjectSequence. (object-array 100) 8 28)) 8 15)
  (?sequence= (s/drop! 20 (ObjectSequence. (object-array 100) 10 50)) 30 30))

(deftest accessing-readable-object-sequences
  (let [s (InputObjectSequence. (object-array (range -50 50)) 10 40)]
    (dotimes [i 40] (?= (s/get s i) (- i 40)))
    (?throws (s/get s -1) ArrayIndexOutOfBoundsException)
    (?throws (s/get s 40) ArrayIndexOutOfBoundsException)))

(deftest accessing-writable-object-sequences
  (let [o (object-array 100)
        s (OutputObjectSequence. o 20 30)]
    (dotimes [i 30] (s/set! s i (- i)))
    (?= (seq (take 30 (drop 20 (seq o)))) (seq (map #(- %) (range 0 30))))
    (?throws (s/set! s -1 0) ArrayIndexOutOfBoundsException)
    (?throws (s/set! s 30 0) ArrayIndexOutOfBoundsException)))

(deftest object-sequencies-cannot-store-nulls
  (?throws (s/set! (OutputObjectSequence. (object-array 10) 0 10) 0 nil) IllegalArgumentException))

(deftest cloning-object-sequence
  (let [a (object-array (range 10))
        s (InputObjectSequence. a 3 5)]
    (?sequence= (.clone s) 3 5 2)
    (?sequence= (s/drop! 2 (.clone s)) 5 3 2)
    (?sequence= s 3 5 2)
    (let [c (.clone s)]
      (aset a 3 100)
      (?= (seq c) (cons 100 (range 4 8))))))

(deftest copy-for-object-buffer
  (let [a (object-array (range 10))
        b (madnet.sequence.ObjectBuffer. a)]
    (.copy b 6 2 3)
    (?= (seq a) [0 1 6 7 8 5 6 7 8 9])))

;;
;; NIO sequence
;;

(deftest nio-sequence-begin-size-and-free-space
  (?sequence= (NIOSequence. (-> (ByteBuffer/allocate 100) (.position 10) (.limit 60)))
               10 50 40))

(deftest nio-sequence-taking-and-droping
  (?sequence= (s/take! 20 (NIOSequence. (ByteBuffer/allocate 50))) 0 20 30)
  (?sequence= (s/drop! 10 (NIOSequence. (ByteBuffer/allocate 50))) 10 40 0))

;;
;; Byte sequence
;;

(deftest byte-sequence-get
  (let [buffer (ByteBuffer/wrap (byte-array (map byte (range -10 10))))
        s (InputByteSequence. (-> buffer (.position 5) (.limit 15)))]
    (dotimes [i 10] (?= (s/get s i) (- i 5)))
    (?throws (s/get s -1) ArrayIndexOutOfBoundsException)
    (?throws (s/get s 10) ArrayIndexOutOfBoundsException)))

(deftest byte-sequence-set
  (let [buffer (-> (ByteBuffer/allocate 20) (.position 7) (.limit 13))
        s (OutputByteSequence. buffer)]
    (dotimes [i 6] (s/set! s i (byte (- i))))
    (dotimes [i 6] (?= (.get buffer (+ i 7)) (- i)))
    (?throws (s/set! s -1 0) ArrayIndexOutOfBoundsException)
    (?throws (s/set! s 6 0) ArrayIndexOutOfBoundsException)
    (?throws (s/set! s 1 \0) IllegalArgumentException)))

;;cloning byte sequence
;;byte buffer copy

;;
;; Char sequence
;;

(deftest char-sequence-get
  (let [buffer (-> (CharBuffer/wrap (char-array "hihello???"))
                   (.position 2)
                   (.limit 8))
        s (InputCharSequence. buffer)]
    (?= (s/get s 0) \h)
    (?= (s/get s 1) \e)
    (?= (s/get s 5) \?)
    (?throws (s/get s -1) ArrayIndexOutOfBoundsException)
    (?throws (s/get s 6) ArrayIndexOutOfBoundsException)))

(deftest char-sequence-set
  (let [buffer (-> (CharBuffer/wrap (char-array 10))
                   (.position 3) 
                   (.limit 7))
        s (OutputCharSequence. buffer)]
    (dotimes [i 4] (s/set! s i (nth "hello" i)))
    (?= (.get buffer 3) \h)
    (?= (.get buffer 4) \e)
    (?= (.get buffer 5) \l)
    (?= (.get buffer 6) \l)))
    
;;cloning char sequence
;;char buffer copy

;;
;; bytes/chars conversion
;;

(defn- unicode-charset [] (java.nio.charset.Charset/forName "UTF-8"))

(deftest converting-simple-byte-sequence-to-chars
  (let [reader (InputByteSequence. (ByteBuffer/wrap (byte-array (map byte (range 97 100)))))
        buffer (CharBuffer/allocate 3)
        writer (OutputCharSequence. buffer)]
    (?= (.readBytes CharSequence$/MODULE$ reader writer (unicode-charset)) (Result. 3 3))
    (?sequence= reader 3 0 0)
    (?sequence= writer 3 0 0)
    (?= (seq (.array buffer)) (seq "abc"))))

(deftest converting-big-byte-sequence-to-char
  (let [reader (InputByteSequence. (ByteBuffer/wrap (byte-array (map byte [-48 -102]))))
        buffer (CharBuffer/allocate 1)
        writer (OutputCharSequence. buffer)]
    (?= (.readBytes CharSequence$/MODULE$ reader writer (unicode-charset)) (Result. 2 1))
    (?= (int (.get buffer 0)) 1050)))

(deftest converting-chars-to-bytes
  (let [reader (InputCharSequence. (CharBuffer/wrap (char-array "hi!!!")))
        buffer (ByteBuffer/allocate 5)
        writer (OutputByteSequence. buffer)]
    (?= (.writeBytes CharSequence$/MODULE$ writer reader (unicode-charset)) (Result. 5 5))
    (?sequence= reader 5 0 0)
    (?sequence= writer 5 0 0)
    (?= (seq (.array buffer)) [104 105 33 33 33])))

(deftest converting-big-char-to-bytes
  (let [reader (InputCharSequence. (CharBuffer/wrap (char-array [(char 1050)])))
        buffer (ByteBuffer/allocate 2)
        writer (OutputByteSequence. buffer)]
    (?= (.writeBytes CharSequence$/MODULE$ writer reader (unicode-charset)) (Result. 1 2))
    (?sequence= reader 1 0 0)
    (?sequence= writer 2 0 0)
    (?= (seq (.array buffer)) [-48 -102])))

;;
;; Circular sequencies
;;

(deftest making-circular-sequence
  (let [s (ObjectSequence. (object-array 20) 8 7)
        c (CircularSequence. s)]
    (?sequence= c 8 7 13)
    (?= (.head c) s)))

(deftest drop-take-and-expand-for-circular-sequence
  (let [s (ObjectSequence. (object-array 20) 0 20)
        c (CircularSequence. s)]
    (s/drop! 12 c)
    (?sequence= c 12 8 12)
    (s/expand! 8 c)
    (?sequence= c 12 16 4)
    (?= (.end c) 8)
    (?sequence= (.head c) 12 8 0)
    (?sequence= (s/drop! 10 c) 2 6 14)))

(deftest get-for-input-circular-sequence
  (let [s (InputObjectSequence. (object-array (range 10)) 5 5)
        c (InputCircularSequence. s)]
    (s/expand! 5 c)
    (?= (s/get c 8) 3)))

(deftest writable-circular-sequence
  (let [a (object-array (range 5))
        s (OutputObjectSequence. a 3 2)
        c (OutputCircularSequence. s)]
    (s/expand! 3 c)
    (s/set! c 4 -1)
    (?= (seq a) [0 1 -1 3 4])))

(deftest free-space-end-and-size-for-circular-sequence-with-compaction-area
  (let [s (ObjectSequence. (object-array (range 10)) 3 5)
        c (CircularSequence. s 3)]
    (?= (s/free-space c) 2))
  (?= (s/free-space (CircularSequence. (ObjectSequence. (object-array 10) 0 5) 3)) 2)
  (?throws (CircularSequence. (ObjectSequence. (object-array 10) 0 10) 1) IllegalArgumentException)
  (let [s (ObjectSequence. (object-array (range 10)) 8 2)
        c (CircularSequence. s 2)]
    (?sequence= (s/expand! 4 c) 8 6 2)
    (?= (.end c) 6)))

(deftest droping-for-circular-sequence-with-compaction-area
  (let [s (ObjectSequence. (object-array (range 10)) 7 3)
        c (CircularSequence. s 2)]
    (s/expand! 2 c)
    (?sequence= (s/drop! 1 (.clone c)) 0 4 4)
    (?sequence= (s/drop! 3 (.clone c)) 2 2 6)))

(deftest compacting-input-circular-sequence
  (let [s (InputObjectSequence. (object-array (range 10)) 6 4)
        c (InputCircularSequence. s 4)]
    (s/drop! 1 c)
    (?= (seq (.buffer c)) (seq (concat [0] (range 7 10) (range 4 10))))))

(deftest output-circular-sequence-with-compaction-threshold
  (let [s (OutputObjectSequence. (object-array (range 10)) 6 4)
        c (OutputCircularSequence. s 4)]
    (?sequence= (s/drop! 1 c) 1 3)
    (?= (seq (.buffer c)) (range 10))
    (?sequence= (s/drop! 2 c) 3 1)
    (?= (seq (.buffer c)) (seq (concat (range 7) (range 1 3) [9])))))

(deftest cloning-circular-sequences
  (let [s (ObjectSequence. (object-array (range 10)) 0 5)
        c (CircularSequence. s 1)]
    (?sequence= (.clone c) 0 5 4)
    (?sequence= (s/drop! 2 (.clone c)) 2 3 6)
    (?sequence= c 0 5 4)
    (?sequence= (.clone (s/expand! 1 (CircularSequence. (ObjectSequence. (object-array 10) 8 2))))
                8 3 7)))

;;cloning readable circular sequencies
;;cloning writable circular sequencies

;;
;; Sequence fabric function
;;

(defmacro ?is [expr type] `(?true (instance? ~type ~expr)))

(deftest sequence-fabric-function-creates-sequence-with-size
  (let [s (s/sequence 10)]
    (?is s IOSequence)
    (?false (.linked s))
    (?is (c/reader s) madnet.sequence.IInputSequence)
    (?is (c/writer s) madnet.sequence.IOutputSequence)))

(deftest sequence-fabric-sequence-metrics
  (let [s (s/sequence 10)]
    (?sequence= s 0 0 0)
    (?sequence= (c/reader s) 0 0 10)
    (?sequence= (c/writer s) 0 10 0)))

(deftest sequence-fabric-with-explicit-reader
  (let [s (s/sequence 10 :reader [2 5])]
    (?sequence= s 2 5 0)
    (?sequence= (c/reader s) 2 5 3)
    (?sequence= (c/writer s) 7 3 0)))

(deftest sequence-fabric-with-explicit-writer
  (let [s (s/sequence 10 :writer [3 5])]
    (?sequence= s 0 3 2)
    (?sequence= (c/reader s) 0 3 7)
    (?sequence= (c/writer s) 3 5 2)))

(deftest sequence-fabric-with-explicit-reader-and-writer
  (let [s (s/sequence 10 :reader [2 5] :writer [4 5])]
    (?sequence= s 2 5 1)
    (?sequence= (c/reader s) 2 5 3)
    (?sequence= (c/writer s) 4 5 1)))

(deftest sequence-fabric-with-sequence-argument
  (let [s (s/sequence (range -10 10))]
    (?= (seq s) (range -10 10))
    (?= (seq (c/reader s)) (range -10 10))))

(deftest sequence-with-element-type
  (let [s (s/sequence 10 :element :object)]
    (?is (c/reader s) InputObjectSequence)
    (?is (c/writer s) OutputObjectSequence))
  (let [s (s/sequence 10 :element :byte)]
    (?is (c/reader s) InputByteSequence)
    (?is (c/writer s) OutputByteSequence))
  (let [s (s/sequence 10 :element :char)]
    (?is (c/reader s) InputCharSequence)
    (?is (c/writer s) OutputCharSequence))
  (?throws (s/sequence 10 :element :unknown) IllegalArgumentException))

(deftest sequence-direct-option
  (?true (-> (s/sequence 10 :element :byte :direct true) (c/reader) .buffer .buffer .isDirect))
  (?false (-> (s/sequence 10 :element :byte :direct false) (c/reader) .buffer .buffer .isDirect))
  (?throws (s/sequence 10 :element :char :direct false) IllegalArgumentException)
  (?throws (s/sequence 10 :direct false) IllegalArgumentException))

(deftest sequence-read-only-and-write-only-options
  (?is (s/sequence 10 :read-only true) InputObjectSequence)
  (?is (s/sequence 10 :write-only true) OutputObjectSequence)
  (?throws (s/sequence 10 :read-only true :write-only true) IllegalArgumentException))

;:circular option

;;auto-close read on emptyness
;;auto-close write on fullness
;;cloning
;;immutable take, drop and expand
;;events

;;
;; Reading/Writing
;;

;from generic input sequence to generic output sequence
;with io sequence
;byte sequence to byte sequence
;char sequence to char sequence

;writing with circular sequences
;;writing with compaction (attention to writer compaction)
