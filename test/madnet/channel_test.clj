(ns madnet.channel-test
  (:require [khazad-dum :refer :all]
            [evil-ant :as e]
            [madnet.channel :as c])
  (:import [madnet.channel Channel ReadableChannel Result]))

;;
;; Base interface
;;

(deftest channels-are-closeable
  (let [c (Channel.)]
    (?true (c/open? c))
    (?throws (c/close c) UnsupportedOperationException)))

(deftest channels-have-events
  (let [c (Channel.)]
    (?= (c/on-close c) nil)
    (?= (c/on-active c) nil)))
    
;;
;; Closing
;;

(defn- channel-with-events [& {:keys [on-active on-close] :or {on-active (e/event)
                                                               on-close (e/event)}}]
  (proxy [Channel] []
    (closeImpl [] nil)
    (onActive [] on-active)
    (onClose [] on-close)))

(deftest channels-close-should-close-events
  (let [c (channel-with-events)] 
    (.close c)
    (?false (e/open? (c/on-close c)))
    (?false (e/open? (c/on-active c)))))

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

(defn- readable-channel []
  (let [on-active (e/switch)]
    (proxy [ReadableChannel] []
      (closeImpl [] nil)
      (onActive [] on-active)
      (readImpl [ch] (if ch (Result. 0 0)))
      (tryPop [] (if (.ready on-active) 42)))))

(deftest readable-channels-have-read-method
  (let [c (ReadableChannel.)]
    (?throws (c/read! c c) UnsupportedOperationException))
  (let [c (readable-channel)]
    (?throws (c/read! c nil) UnsupportedOperationException)))

(deftest read-returns-result-structure
  (let [c (readable-channel)]
    (?= (c/read! c c) (Result/Zero))))

(deftest readable-channels-have-queue-pop-interface
  (let [c (ReadableChannel.)]
    (?throws (c/pop! c) UnsupportedOperationException)
    (?throws (c/pop-in! c 10) UnsupportedOperationException)
    (?throws (c/try-pop! c) UnsupportedOperationException)))

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
;; Writeable channels
;;

;writeable channels have write methods (throws exception by default)
;;write must return result structure
;writeable channels have push interface
;;all functions implemented using tryPush
;;default push interface uses onActive

;read and write co-operation

;;
;; IO channels
;;

;have reader and writer
;have read/write, push/pop methods
;;have no onAcitve
;;operations implemented using reader and writer
