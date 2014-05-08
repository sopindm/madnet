(ns madnet.channel-test
  (:require [khazad-dum :refer :all]
            [evil-ant :as e]
            [madnet.channel :as c])
  (:import [madnet.channel Channel ReadableChannel WritableChannel IOChannel Result]))

;;
;; Base interface
;;

(defmacro ?unsupported [& forms]
  `(do ~@(map (fn [form] `(?throws ~form UnsupportedOperationException)) forms)))

(deftest channels-are-closeable
  (let [c (Channel.)]
    (?true (c/open? c))
    (?unsupported (c/close c))))

(deftest channels-have-events
  (let [c (Channel.)]
    (?= (c/on-close c) nil)
    (?= (c/on-active c) nil)))
    
;;
;; Closing
;;

(defn- channel-with-events [& {:keys [on-active on-close] :or {on-active (e/event)
                                                               on-close (e/event)}}]
  (let [open? (atom true)]
    (proxy [Channel] []
      (isOpen [] @open?)
      (closeImpl [] (reset! open? false))
      (onActive [] on-active)
      (onClose [] on-close))))

(deftest channels-close-should-close-events
  (let [c (channel-with-events)] 
    (.close c)
    (?false (e/open? (c/on-close c)))
    (?false (e/open? (c/on-active c)))))

(deftest its-safe-to-close-channel-twice
  (with-open [c (channel-with-events)]
    (.close c)))

(deftest its-thread-safe-to-close-channel
  (let [channels (repeatedly 20 #(channel-with-events))
        futures (doall (repeatedly 100 #(future (Thread/yield) (doseq [c channels] (c/close c)))))]
    (doall (map deref futures))))

(defmacro ?emit= [form event what]
  `(let [event# ~event
         actions# (atom [])]
     (with-open [handler# (e/handler ([e# s#] (swap! actions# conj [e# s#])) event#)]
       ~form
       (?= @actions# ~what))))

(defmacro ?emits [form event [emitter source]] `(?emit= ~form ~event [[~emitter ~source]]))

(deftest channels-close-emits-on-close
  (let [c (channel-with-events) event (c/on-close c)]
    (?emits (.close c) event [event c])))

;;
;; Registration
;;

(deftest channels-can-register-themselves-in-signal-sets
  (let [c (Channel.)]
    (with-open [s (e/signal-set)]
      (c/register c s)
      (?= (count (e/absorbers s)) 0))))

(deftest channel-default-implementation-register-no-null-signals
  (with-open [s (e/signal-set)]
    (let [c (channel-with-events :on-active (e/switch) :on-close (e/switch))]
      (c/register c s)
      (?= (set (e/absorbers s)) #{(c/on-active c) (c/on-close c)})))
  (with-open [c (channel-with-events :on-active (e/switch))
              s (e/signal-set)]
    (c/register c s)
    (?= (set (e/absorbers s)) #{(c/on-active c)})))

;;
;; Readable channels
;;

(defn- readable-channel [& {:keys [readable] :or {readable true}}]
  (let [open? (atom true)
        on-active (e/switch)]
    (proxy [ReadableChannel] []
      (isOpen [] @open?)
      (closeImpl [] (reset! open? false))
      (onActive [] on-active)
      (readImpl [ch] (if (and readable ch) (Result. 0 1)))
      (tryPop [] (if (.ready on-active) 42)))))

(defn- writable-channel [& {:keys [writable] :or {writable true}}]
  (let [open? (atom true)
        on-active (e/switch)]
    (proxy [WritableChannel] []
      (isOpen [] @open?)
      (closeImpl [] (reset! open? false))
      (onActive [] on-active)
      (writeImpl [ch] (if (and writable ch) (Result. 1 0)))
      (tryPush [obj] (.ready on-active)))))

(deftest readable-channels-have-read-method
  (?unsupported (c/read! (ReadableChannel.) (WritableChannel.))
                (c/read! (ReadableChannel.) nil)))

(deftest readable-channels-have-reader
  (let [c (ReadableChannel.)]
    (?= (c/reader c) c)))

(deftest read-returns-result-structure
  (let [c (readable-channel)]
    (?= (c/read! c (writable-channel)) (Result. 0 1))))

(deftest readable-channels-have-queue-pop-interface
  (let [c (ReadableChannel.)]
    (?unsupported (c/pop! c) (c/pop-in! c 10) (c/try-pop! c))))

(deftest pop-interface-implemented-with-try-pop
  (with-open [c (readable-channel)]
    (e/turn-on! (c/on-active c))
    (?= (c/try-pop! c) 42)
    (?= (c/pop-in! c 10) 42)
    (?= (c/pop! c) 42))
  (with-open [c (readable-channel)]
    (let [f (future (c/pop-in! c 6))]
      (Thread/sleep 2)
      (?false (realized? f))
      (?= @f nil))
    (let [f (future (c/pop-in! c 6))]
      (Thread/sleep 2)
      (?false (realized? f))
      (e/turn-on! (c/on-active c))
      (?= @f 42))
    (e/turn-off! (c/on-active c))
    (let [f (future (c/pop! c))]
      (Thread/sleep 2)
      (?false (realized? f))
      (e/turn-on! (c/on-active c))
      (?= @f 42))))

(deftest pop-iterface-functions-uses-on-active
  (with-open [c (readable-channel)]
    (e/turn-on! (c/on-active c))
    (?emits (c/pop! c) (c/on-active c) [(c/on-active c) c])
    (?emits (c/pop-in! c 10) (c/on-active c) [(c/on-active c) c])))

;;
;; Writable channels
;;

(deftest writable-channels-have-write-method
  (?unsupported (c/write! (WritableChannel.) (ReadableChannel.))
                (c/write! (WritableChannel.) nil)))

(deftest writable-channels-have-writer
  (let [c (WritableChannel.)]
    (?= (c/writer c) c)))

(deftest write-returns-result-structure
  (let [c (writable-channel)]
    (?= (c/write! c (readable-channel)) (Result. 1 0))))

(deftest writable-channels-have-queue-push-interface
  (let [c (WritableChannel.)]
    (?unsupported (c/push! c 42) (c/push-in! c (atom 42) 10) (c/try-push! c "42"))))

(deftest push-interface-implemented-with-try-push
  (with-open [c (writable-channel)]
    (e/turn-on! (c/on-active c))
    (?true (c/try-push! c 42))
    (?true (c/push-in! c (atom 42) 10))
    (c/push! c [42]))
  (with-open [c (writable-channel)]
    (let [f (future (c/push-in! c 42 6))]
      (Thread/sleep 2)
      (?false (realized? f))
      (?= @f false))
    (let [f (future (c/push-in! c 42 6))]
      (Thread/sleep 2)
      (?false (realized? f))
      (e/turn-on! (c/on-active c))
      (?= @f true))
    (e/turn-off! (c/on-active c))
    (let [f (future (c/push! c 42))]
      (Thread/sleep 2)
      (?false (realized? f))
      (e/turn-on! (c/on-active c))
      (?= @f nil))))

(deftest push-iterface-functions-uses-on-active
  (with-open [c (writable-channel)]
    (e/turn-on! (c/on-active c))
    (?emits (c/push! c 42) (c/on-active c) [(c/on-active c) c])
    (?emits (c/push-in! c 42 10) (c/on-active c) [(c/on-active c) c])))

(deftest replacing-read-by-write-and-otherwise
  (?= (c/write! (writable-channel :writable false) (readable-channel)) (Result. 0 1))
  (?= (c/read! (readable-channel :readable false) (writable-channel)) (Result. 1 0)))

;;
;; IO channels
;;

(deftest io-channels-have-reader-and-writer
  (let [c (IOChannel.)]
    (?= (c/reader c) nil)
    (?= (c/writer c) nil)))

(deftest io-channels-have-read-and-write
  (let [c (IOChannel.)]
    (?unsupported (c/write! c (ReadableChannel.))
                  (c/write! (WritableChannel.) c))))

(deftest io-channels-have-push-and-pop-methods
  (let [c (IOChannel.)]
    (?unsupported (c/push! c 123) (c/push-in! c 123 10) (c/try-push! c 123)
                  (c/pop! c) (c/pop-in! c 10) (c/try-pop! c))))

(deftest io-channels-have-no-on-active
  (?= (c/on-active (IOChannel.)) nil))

(defn io-channel [& {:keys [reader writer] :or {reader (readable-channel) writer (writable-channel)}}]
  (let [open? (atom true)
        on-close (e/event)]
    (proxy [IOChannel] []
      (isOpen [] @open?)
      (closeImpl [] (reset! open? false))
      (onClose [] on-close)
      (reader [] reader)
      (writer [] writer))))

(deftest io-channel-close-closed-reader-and-writer
  (with-open [c (io-channel)]
    (?true (instance? ReadableChannel (c/reader c)))
    (?true (instance? WritableChannel (c/writer c)))
    (c/close c)
    (?false (c/open? c))
    (?false (e/open? (c/on-close c)))
    (?false (c/open? (c/reader c)))
    (?false (c/open? (c/writer c)))))

(defn simple-reader []
  (proxy [ReadableChannel] []
    (closeImpl [] nil)
    (readImpl [ch] (Result. 10 11))
    (pop [] 1)
    (popIn [time] time)
    (tryPop [] 3)))

(defn simple-writer []
  (proxy [WritableChannel] []
    (closeImpl [] nil)
    (writeImpl [ch] (Result. 100 111))
    (push [o] nil)
    (pushIn [o time] (= (mod time 2) 0))
    (tryPush [o] (= (mod o 2) 0))))

(deftest reading-from-io-channel
  (with-open [c (io-channel :reader (simple-reader))]
    (?= (c/read! c (WritableChannel.)) (Result. 10 11))
    (?= (c/pop! c) 1)
    (?= (c/pop-in! c 100500) 100500)
    (?= (c/try-pop! c) 3)))

(deftest writing-to-io-channel
  (with-open [c (io-channel :writer (simple-writer))]
    (?= (c/write! c (ReadableChannel.)) (Result. 100 111))
    (?= (c/push! c 123) nil)
    (?= (c/push-in! c 123 102030) true)
    (?= (c/push-in! c 123 102031) false)
    (?= (c/try-push! c 123) false)
    (?= (c/try-push! c 124) true)))

(deftest io-channels-co-operation
  (?= (c/write! (io-channel) (readable-channel :readable false)) (Result. 1 0))
  (?= (c/write! (io-channel :writer (writable-channel :writable false)) (readable-channel))
      (Result. 0 1))
  (?= (c/read! (io-channel) (writable-channel :writable false)) (Result. 0 1))
  (?= (c/read! (io-channel :reader (readable-channel :readable false)) (writable-channel))
      (Result. 1 0)))

