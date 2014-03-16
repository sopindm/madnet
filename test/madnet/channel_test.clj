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
    (c/push! p [123 456])
    (?= (c/pop! p) [123 456])))

;writing/reading to/from object pipe


