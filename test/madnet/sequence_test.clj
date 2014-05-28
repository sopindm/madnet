(ns madnet.sequence-test
  (:require [khazad-dum :refer :all]
            [madnet.channel-test :refer [?unsupported]]
            [madnet.channel :as c] 
            [madnet.sequence :as s])
  (:import [java.nio ByteBuffer CharBuffer]
            [madnet.channel Result]
           [madnet.sequence Sequence ReadableSequence WritableSequence IOSequence
                            ObjectSequence ReadableObjectSequence WritableObjectSequence
                            NIOSequence
                            ReadableByteSequence WritableByteSequence
                            ReadableCharSequence WritableCharSequence]))

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
  (?unsupported (s/get (ReadableSequence.) 10)))

(defn readable-sequence
  ([seq] (readable-sequence seq (count seq)))
  ([seq size] (readable-sequence seq 0 size))
  ([seq begin size & [result]]
     (let [begin (atom begin) size (atom size)]
       (proxy [ReadableSequence] []
         (size [] @size)
         (begin [] @begin)
         (freeSpace [] (- (count seq) @begin @size))
         (drop [n] (swap! begin + n) (swap! size - n))
         (expand [n] (swap! size + n))
         (get [n] (nth seq (+ @begin n)))
         (readImpl [ch] (or result (proxy-super readImpl ch)))))))

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
  ([seq begin size & [result]]
     (let [seq (atom seq) begin (atom begin) size (atom size)]
       (proxy [WritableSequence Iterable] []
         (begin [] @begin)
         (size [] @size)
         (freeSpace [] (- (count @seq) @begin @size))
         (drop [n] (swap! begin + n) (swap! size - n))
         (expand [n] (swap! size + n))
         (set [n value] (let [n (+ n @begin)]
                          (swap! seq #(concat (take n %) [value] (drop (inc n) %)))))
         (iterator [] (.iterator @seq))
         (writeImpl [ch] (or result (proxy-super writeImpl ch)))))))

(deftest writable-sequence-push-implementation-uses-set-and-expand
  (let [s (writable-sequence (repeat 3 nil) 2)]
    (?= (c/try-push! s 100) true)
    (?= (c/try-push! s 500) true)
    (?= (c/try-push! s 100500) false)
    (?= (seq s) [100 500 nil])))

(deftest default-read-implementation-can-write-to-writable-sequence
  (let [reader (readable-sequence (range 5))
        writer (writable-sequence (repeat 10 nil) 8)]
    (?= (c/read! reader writer) (Result. 5))
    (?= (seq reader) nil)
    (?= (seq writer)
        (seq (concat (range 5) (repeat 5 nil)))))
  (let [reader (readable-sequence (range 10) 2 5)
        writer (writable-sequence (repeat 5 nil) 1 2)]
    (?= (c/read! reader writer) (Result. 2))
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

(deftest io-sequence-read-reads-from-reader
  (let [s (io-sequence (readable-sequence (range 5)) nil)
        writer (writable-sequence (repeat 3 nil))]
    (?= (c/read! s writer) (Result. 3 3))
    (?sequence= (c/reader s) 3 2))
  (let [s (io-sequence (readable-sequence (range 5)) (writable-sequence (range 5)))
        writer (writable-sequence (repeat 3 nil))]
    (c/read! s writer)
    (?sequence= (c/writer s) 0 5)))

(deftest io-sequence-write-writes-to-writer
  (let [s (io-sequence nil (writable-sequence (repeat 5 nil)))
        reader (readable-sequence (repeat 3 nil))]
    (?= (c/write! s reader) (Result. 3 3))
    (?sequence= (c/writer s) 3 2)))

(deftest io-sequence-write-expands-reader
  (let [s (io-sequence (readable-sequence (range 5) 2) (writable-sequence (repeat 3 nil)))
        reader (readable-sequence (range -10 0))]
    (?= (c/write! s reader) (Result. 3 3))
    (?sequence= (c/writer s) 3 0)
    (?sequence= (c/reader s) 0 5)
    (?= (seq s) (seq (range 5)))))

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

(deftest reading-from-linked-io-sequence-expands-writer
  (let [s (io-sequence (readable-sequence (range 5)) (writable-sequence (repeat 10 nil) 5) :linked true)
        writer (writable-sequence (repeat 10 nil))]
    (?= (c/read! s writer) (Result. 5 5))
    (?sequence= (c/writer s) 0 10)))

(deftest io-sequence-read-with-no-symmetric-result
  (let [s (io-sequence (readable-sequence (range 5) 0 5 (Result. 3 4))
                       (writable-sequence (range 10) 1 2) :linked true)]
    (?= (c/read! s (writable-sequence (range 5))) (Result. 3 4))
    (?sequence= (c/writer s) 1 5)))

(deftest io-sequence-write-with-no-symmetric-result
  (let [s (io-sequence (readable-sequence (range 10) 2 3)
                       (writable-sequence (range 5) 0 5 (Result. 2 4)))]
    (?= (c/write! s (readable-sequence (range 100))) (Result. 2 4))
    (?sequence= (c/reader s) 2 7)))

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
  (let [s (ReadableObjectSequence. (object-array (range -50 50)) 10 40)]
    (dotimes [i 40] (?= (s/get s i) (- i 40)))
    (?throws (s/get s -1) ArrayIndexOutOfBoundsException)
    (?throws (s/get s 40) ArrayIndexOutOfBoundsException)))

(deftest accessing-writable-object-sequences
  (let [o (object-array 100)
        s (WritableObjectSequence. o 20 30)]
    (dotimes [i 30] (s/set! s i (- i)))
    (?= (seq (take 30 (drop 20 (seq o)))) (seq (map #(- %) (range 0 30))))
    (?throws (s/set! s -1 0) ArrayIndexOutOfBoundsException)
    (?throws (s/set! s 30 0) ArrayIndexOutOfBoundsException)))

(deftest object-sequencies-cannot-store-nulls
  (?throws (s/set! (WritableObjectSequence. (object-array 10) 0 10) 0 nil) IllegalArgumentException))

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
        s (ReadableByteSequence. (-> buffer (.position 5) (.limit 15)))]
    (dotimes [i 10] (?= (s/get s i) (- i 5)))
    (?throws (s/get s -1) ArrayIndexOutOfBoundsException)
    (?throws (s/get s 10) ArrayIndexOutOfBoundsException)))

(deftest byte-sequence-set
  (let [buffer (-> (ByteBuffer/allocate 20) (.position 7) (.limit 13))
        s (WritableByteSequence. buffer)]
    (dotimes [i 6] (s/set! s i (byte (- i))))
    (dotimes [i 6] (?= (.get buffer (+ i 7)) (- i)))
    (?throws (s/set! s -1 0) ArrayIndexOutOfBoundsException)
    (?throws (s/set! s 6 0) ArrayIndexOutOfBoundsException)
    (?throws (s/set! s 1 \0) IllegalArgumentException)))

(deftest byte-sequence-can-write-to-any-sequence
  (let [bs (ReadableByteSequence. (ByteBuffer/wrap (byte-array (map byte (range 10)))))
        os (writable-sequence (repeat 5 nil))]
    (c/read! bs os)
    (?= (take 5 (seq os)) (seq (range 5))))
  (let [src (ReadableByteSequence. (ByteBuffer/wrap (byte-array (map byte (range 5 10)))))
        buffer (ByteBuffer/wrap (byte-array 3))
        dst (WritableByteSequence. buffer)]
    (c/read! src dst)
    (?= (seq src) (range 8 10))
    (?= (.get buffer 0) 5)
    (?= (.get buffer 1) 6)
    (?= (.get buffer 2) 7)))

(deftest byte-sequence-can-read-from-other-sequences
  (let [src (readable-sequence (map byte (range 10)))
        buffer (ByteBuffer/wrap (byte-array 10))
        dst (WritableByteSequence. buffer)]
    (c/write! dst src)
    (dotimes [i 10] (?= (.get buffer i) i))))

;;
;; Char sequence
;;

(deftest char-sequence-get
  (let [buffer (-> (CharBuffer/wrap (char-array "hihello???"))
                   (.position 2)
                   (.limit 8))
        s (ReadableCharSequence. buffer)]
    (?= (s/get s 0) \h)
    (?= (s/get s 1) \e)
    (?= (s/get s 5) \?)
    (?throws (s/get s -1) ArrayIndexOutOfBoundsException)
    (?throws (s/get s 6) ArrayIndexOutOfBoundsException)))

(deftest char-sequence-set
  (let [buffer (-> (CharBuffer/wrap (char-array 10))
                   (.position 3) 
                   (.limit 7))
        s (WritableCharSequence. buffer)]
    (dotimes [i 4] (s/set! s i (nth "hello" i)))
    (?= (.get buffer 3) \h)
    (?= (.get buffer 4) \e)
    (?= (.get buffer 5) \l)
    (?= (.get buffer 6) \l)))
    
(deftest char-sequence-can-write-to-any-sequence
  (let [buffer (CharBuffer/wrap (char-array "hello"))
        reader (ReadableCharSequence. buffer)
        writer (writable-sequence (repeat 5 nil))]
    (c/read! reader writer)
    (?sequence= reader 5 0 0)
    (?= (apply str (seq writer)) "hello"))
  (let [reader (ReadableCharSequence. (CharBuffer/wrap (char-array "hi again")))
        buffer (CharBuffer/allocate 5)
        writer (WritableCharSequence. buffer)]
    (c/read! reader writer)
    (?sequence= reader 5 3 0)
    (?sequence= writer 5 0 0)
    (?= (.get buffer 0) \h)
    (?= (.get buffer 4) \g)))

(deftest char-sequence-can-read-from-any-sequence-with-chars
  (let [buffer (CharBuffer/allocate 10)
        writer (WritableCharSequence. buffer)
        reader (readable-sequence "hello")]
    (c/write! writer reader)
    (?= (take 5 (seq (.array buffer))) (seq "hello"))))

;;
;; bytes/chars conversion
;;

(defn- unicode-charset [] (java.nio.charset.Charset/forName "UTF-8"))

(deftest converting-simple-byte-sequence-to-chars
  (let [reader (ReadableByteSequence. (ByteBuffer/wrap (byte-array (map byte (range 97 100)))))
        buffer (CharBuffer/allocate 3)
        writer (WritableCharSequence. buffer)]
    (?= (.writeBytes writer reader (unicode-charset)) (Result. 3 3))
    (?sequence= reader 3 0 0)
    (?sequence= writer 3 0 0)
    (?= (seq (.array buffer)) (seq "abc"))))

(deftest converting-big-byte-sequence-to-char
  (let [reader (ReadableByteSequence. (ByteBuffer/wrap (byte-array (map byte [-48 -102]))))
        buffer (CharBuffer/allocate 1)
        writer (WritableCharSequence. buffer)]
    (?= (.writeBytes writer reader (unicode-charset)) (Result. 2 1))
    (?= (int (.get buffer 0)) 1050)))

(deftest converting-chars-to-bytes
  (let [reader (ReadableCharSequence. (CharBuffer/wrap (char-array "hi!!!")))
        buffer (ByteBuffer/allocate 5)
        writer (WritableByteSequence. buffer)]
    (?= (.readBytes reader writer (unicode-charset)) (Result. 5 5))
    (?sequence= reader 5 0 0)
    (?sequence= writer 5 0 0)
    (?= (seq (.array buffer)) [104 105 33 33 33])))

(deftest converting-big-char-to-bytes
  (let [reader (ReadableCharSequence. (CharBuffer/wrap (char-array [(char 1050)])))
        buffer (ByteBuffer/allocate 2)
        writer (WritableByteSequence. buffer)]
    (?= (.readBytes reader writer (unicode-charset)) (Result. 1 2))
    (?sequence= reader 1 0 0)
    (?sequence= writer 2 0 0)
    (?= (seq (.array buffer)) [-48 -102])))

;chars to bytes (equal size)
;chars to bytes (not enought space in byte buffer)
;chars to bytes (too much space in byte buffer)
;chars to bytes (not 1 to 1)
;chars to bytes (not enough space for last char)

;;
;;sequence features
;;

;;buffer (sequence factory)
;;factory function
;;thread safety
;;auto-close read on emptyness
;;auto-close write on fullness
;;immutable take, drop and expand
;;events

;;
;;circular ranges
;;

;;circular ranges reuse beginning of underlying buffer
;;circular ranges can have compaction ratio
