(ns madnet.channel-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c])
  (:import [madnet.channel Channel]))

(deftest channels-are-closeable
  (let [c (Channel.)]
    (?true (c/open? c))
    (?true (c/readable? c))
    (?true (c/writeable? c))
    (?throws (c/close c) UnsupportedOperationException)
    (?throws (c/close-read c) UnsupportedOperationException)
    (?throws (c/close-write c) UnsupportedOperationException)))

(deftest channels-are-readable-and-writeable
  (let [c (Channel.)]
    (?throws (c/read! c nil) UnsupportedOperationException)
    (?throws (c/write! c nil) UnsupportedOperationException)))

;channels have some events (nulls by default)

(deftest channels-have-queue-interface
  (let [c (Channel.)]
    (?throws (c/tryPush! c 42) UnsupportedOperationException)
    (?throws (c/push! c 42) UnsupportedOperationException)
    (?throws (c/push-in! c 42 10) UnsupportedOperationException)
    (?throws (c/tryPop! c) UnsupportedOperationException)
    (?throws (c/pop! c) UnsupportedOperationException)
    (?throws (c/pop-in! c 42) UnsupportedOperationException)))
    
;channel have default implementation for push/pushIn/pop/popIn
;channel default implementation for push/pushIn/pop/popIn tries to use events

;channel read/write returns result structure (with read/writen info)

