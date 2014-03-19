(ns madnet.channel-test
  (:require [madnet.range-test :refer [?range=]]
            [madnet.channel :as c]
            [madnet.range :as r]
            [madnet.sequence :as s]
            [madnet.event :as e]
            [khazad-dum :refer :all])
  (:import [madnet.channel Result]
           [java.nio.channels ClosedChannelException]))

(deftest making-object-pipe
  (with-open [p (c/object-pipe)]
    (c/push! p [123 456] :timeout 0)
    (?= (c/pop! p) [123 456])
    (?= (c/pop! p :timeout 0) nil)))

(deftest writing-and-reading-for-object-pipes
  (with-open [p (c/object-pipe)]
    (?= (c/write! p (s/wrap (range 0 10 3))) (Result. 4))
    (?= (c/write! p (s/wrap [])) (Result. 0))
    (let [s (s/sequence 10)]
      (?= (c/read! p s) (Result. 4))
      (?= (seq s) [0 3 6 9])
      (?= (c/read! p s) (Result. 0))
      (?= (seq s) [0 3 6 9]))))

(deftest writing-from-object-pipe-to-full-sequence
  (with-open [p (c/object-pipe)]
    (c/push! p 123)
    (let [s (s/sequence 0)]
      (?= (c/read! p s) (Result. 0))
      (?= (seq s) nil)
      (?= (c/pop! p :timeout 0) 123))))

(deftest pushing-to-full-pipe
  (with-open [p (c/object-pipe 1)]
    (c/push! p 123)
    (?= (c/push! p 234 :timeout 0) nil)))

(deftest writing-to-full-pipe
  (with-open [p (c/object-pipe 1)]
    (let [s (s/wrap [1 2 3])]
      (?= (c/write! p s) (Result. 1))
      (?= (seq s) [2 3])
      (?= (c/write! p s) (Result. 0))
      (?= (seq s) [2 3]))))

;object pipe registering, registering closed pipe

(deftest closing-object-pipe
  (let [p (c/object-pipe)]
    (.close p)
    (?throws (c/push! p 123) java.nio.channels.ClosedChannelException)
    (?throws (c/pop! p) java.nio.channels.ClosedChannelException)
    (?throws (c/write! p (s/sequence 10))
             java.nio.channels.ClosedChannelException)
    (?throws (c/read! p (s/sequence 10))
             java.nio.channels.ClosedChannelException)))

;closing object pipe (cannot read/write for closed pipe, pipe onClose event)

;object pipe reader/writer closing
;reading from closed writer drains buffer and closes
;writing to closed pipe closed

;object pipe read/write events

;thread-safety
