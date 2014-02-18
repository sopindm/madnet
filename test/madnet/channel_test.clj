(ns madnet.channel-test
  (:require [madnet.range-test :refer [?range=]]
            [madnet.channel :as c]
            [madnet.range :as r]
            [madnet.sequence :as s]
            [khazad-dum :refer :all])
  (:import [madnet.channel Result]))

(deftest making-pipe
  (let [p (c/pipe)]
    (?= (c/write! p (s/wrap (byte-array (map byte (range 10)))))
        (Result. 10 10))
    (let [s (s/sequence [10 :element :byte])]
      (?= (c/read! p s) (Result. 10 10))
      (?= (seq s) (range 10)))))

(deftest reading-from-empty-pipe
  (let [p (c/pipe)
        reader (agent p)
        s (s/sequence [10 :element :byte])]
    (send-off reader #(c/read % s))
    (if-not (await-for 1000 reader)
      (throw (RuntimeException. "Agent timeout")))))

(deftest overwriting-pipe
  (let [p (c/pipe)
        s (s/wrap (byte-array 1000000))
        writer (agent s)]
    (send-off writer #(c/write p %))
    (if (await-for 1000 writer) (?true (> (count s) 0))
        (throw (RuntimeException. "Agent timeout")))))

;pipe write and read result

;closing pipe
;writing to closed pipe

;overwriting pipe
;overreading pipe
(java.nio.channels.Pipe/open)

 

