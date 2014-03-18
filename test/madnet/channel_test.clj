(ns madnet.channel-test
  (:require [madnet.range-test :refer [?range=]]
            [madnet.channel :as c]
            [madnet.range :as r]
            [madnet.sequence :as s]
            [madnet.event :as e]
            [khazad-dum :refer :all])
  (:import [madnet.channel Result]))

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

;writing to full pipe

;object pipe size (default in unlimited, writing/pushing to full pipe)
;closing object pipe
;object pipe read/write events

;thread-safety
