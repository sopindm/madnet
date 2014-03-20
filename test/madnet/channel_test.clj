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
    (?= (c/push! p 234 :timeout 0) nil)
    (?= (c/pop! p) 123)
    (?= (c/push! p 234 :timeout 0) p)))

(deftest writing-to-full-pipe
  (with-open [p (c/object-pipe 1)]
    (let [s (s/wrap [1 2 3])]
      (?= (c/write! p s) (Result. 1))
      (?= (seq s) [2 3])
      (?= (c/write! p s) (Result. 0))
      (?= (seq s) [2 3]))))

(deftest registering-object-pipe
  (with-open [p (c/object-pipe)
              s (e/event-set)]
    (c/register p s)
    (.close p)
    (?throws (c/register p s) java.nio.channels.ClosedChannelException)))

(deftest closing-object-pipe
  (let [p (c/object-pipe)]
    (.close p)
    (?throws (c/push! p 123) java.nio.channels.ClosedChannelException)
    (?throws (c/pop! p) java.nio.channels.ClosedChannelException)
    (?throws (c/write! p (s/sequence 10))
             java.nio.channels.ClosedChannelException)
    (?throws (c/read! p (s/sequence 10))
             java.nio.channels.ClosedChannelException)))
        
(deftest reading-object-pipe-with-closed-writer
  (with-open [p (c/object-pipe)]
    (let [r (.reader p)
          w (.writer p)]
      (c/write! w (s/wrap (range 5)))
      (.close w)
      (let [s (s/sequence 10)]
        (c/read! r s)
        (?= (seq s) (range 5))
        (?false (c/open? r)))))
  (with-open [p (c/object-pipe)]
    (let [r (.reader p)
          w (.writer p)]
      (c/push! w 123)
      (.close w)
      (?= (c/pop! r) 123)
      (?true (c/open? r))
      (?= (c/pop! r :timeout 0) nil)
      (?false (c/open? r)))))

(deftest writing-to-closed-reader
  (with-open [p (c/object-pipe)]
    (let [r (.reader p)
          w (.writer p)]
      (c/write! w (s/wrap (range 10)))
      (.close r)
      (?= (c/push! w 123 :timeout 0) nil)
      (?false (c/open? w))))
  (with-open [p (c/object-pipe)]
    (let [r (.reader p)
          w (.writer p)]
      (.close r)
      (?= (c/write! w (s/wrap (range 10))) (Result. 0 0))
      (?false (c/open? w)))))

;object pipe read/write events

;thread-safety
;;multiple producters/consumers (read what was writen, queue is empty at the end)
;;wire closing visible to other end
