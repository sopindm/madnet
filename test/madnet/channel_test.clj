(ns madnet.channel-test
  (:require [khazad-dum :refer :all]
            [evil-ant :as e]
            [madnet.channel :as c])
  (:import [madnet.channel Channel]))

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

;readable and writeable channels

;;
;; Push/pop
;;

;channel have default implementation for push/pushIn/pop/popIn
;channel default implementation for push/pushIn/pop/popIn tries to use events

;;
;; Read/write
;;

;readable channels are readable, have queue pop interface
;writeable channels are writeable, have queue push interface
;channel read/write returns result structure (with read/writen info)
;channel read/write is co-operations (should provide just one of them)

;io channels have writer and reader (and channels themselve)
