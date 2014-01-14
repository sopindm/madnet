(ns madnet.sequence.primitive-test
  (:require [khazad-dum.core :refer :all]
            [madnet.sequence.primitive :as p]
            [madnet.sequence-test :use [?sequence=]])
  (:import [madnet.sequence IBuffer]))

(deftest byte-array-as-a-buffer
  (let [array (p/byte-array 128)]
    (?true (isa? (type array) IBuffer))))

  