(ns madnet.sequence.nio-test
  (:require [khazad-dum.core :refer :all]
            [madnet.sequence :as s]
            [madnet.sequence.nio :as n]
            [madnet.sequence-test :use [?sequence=]])
  (:import [madnet.sequence IBuffer ASequence]))

(deftest making-byte-buffers
  (let [buffer (n/byte-buffer 128)]
    (?true (isa? (type buffer) IBuffer))
    (?true (isa? (type (s/sequence buffer)) ASequence))
    (?sequence= (s/sequence buffer 10 20) [10 20])))

;writing byte to buffer
;writing buffers to buffers
;reading byte from buffer
;reading buffer from buffer

;writing/reading other types
;converting seq to seq for other buffer

;seq from byte buffer sequence
;buffer sequence random access

;buffers for other types
;generic buffer function

