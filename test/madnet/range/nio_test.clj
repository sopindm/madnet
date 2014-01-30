(ns madnet.range.nio-test
  (:require [khazad-dum.core :refer :all]
            [madnet.range-test :refer :all]
            [madnet.range :as r]
            [madnet.range.nio :as n])
  (:import [java.nio ByteBuffer]
           [madnet.range]
           [madnet.range.nio]))

(defmacro ?buffer= [expr position limit capacity]
  `(do (?= (.position ~expr) ~position)
       (?= (.limit ~expr) ~limit)
       (?= (.capacity ~expr) ~capacity)))

(deftest making-nio-range
  (let [b (ByteBuffer/allocate 1024)
        r (n/range 128 512 b)]
    (?range= r [128 512])
    (?buffer= (.buffer r) 128 512 1024)))

(deftest cloning-nio-range
  (let [b (ByteBuffer/allocate 1024)
        r (n/range 64 256 b)]
    (?range= (.clone r) [64 256])
    (?true (identical? (.buffer r) (.buffer (.clone r))))))

;nio range clone
;nio range operations (check begin and end changes)
;nio range outside of buffer

(deftest making-byte-range
  (let [r (n/byte-range 128 512 (ByteBuffer/allocate 1024))]
    (?range= r [128 512])))

;equality and cloning
;making range outsize of buffer
;expanding range too much

;circular nio range

