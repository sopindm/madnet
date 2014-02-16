(ns madnet.range.LinkedRange-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c] 
            [madnet.range :as r]
            [madnet.range-test :use [irange]])
  (:import [madnet.range IntegerRange ProxyRange LinkedRange]
           [madnet.channel Result]))

(defn- link! [range prev next]
  (LinkedRange. range prev next))

(deftest make-linked-range
  (let [r1 (irange 0 10)
        r2 (irange 10 15)
        r3 (irange -7 0)
        l1 (link! r1 r3 r2)
        l2 (link! r2 r1 nil)
        l3 (link! r3 nil r1)]
    (?= (r/size l1) 10)
    (?= (r/size l2) 5)
    (?= (r/size l3) 7)
    (?true (isa? (type l1) ProxyRange))))

(deftest linked-range-cloning
  (let [r1 (irange 0 10)
        r2 (irange 10 15)
        r3 (irange 10 15)
        lr (link! r1 r3 r2)
        lc (.clone lr)]
    (r/drop! 5 r1)
    (?range= (.range lc) [0 10])
    (r/drop! 5 r2)
    (?range= (.next lc) [15 15])
    (r/drop! 3 r3)
    (?range= (.prev lc) [13 15])))

(deftest linked-range-operations
  (let [r1 (irange 0 10)
        r2 (irange 10 20)
        r3 (irange -10 0)
        lr (link! r1 r3 r2)]
    (r/drop! 3 lr)
    (?range= r3 [-10 3])
    (r/expand! 7 lr)
    (?range= r2 [17 20])))

(deftest writing-and-reading-for-linked-ranges
  (letfn [(writeable [begin end]
            (proxy [IntegerRange] [begin end]
              (write [ch] 
                (if (isa? (type ch) IntegerRange)
                  (do (.drop this 2)
                      (.drop ch 2)
                      (Result. 2 2))))))]
    (let [dst (writeable 0 5)
          dst-prev (irange -5 0)
          src (writeable 0 5)
          src-prev (irange -5 0)
          dst (LinkedRange. dst dst-prev nil)
          src (LinkedRange. src src-prev nil)]
      (?= (c/write! dst src) (Result. 2 2))
      (?range= dst [2 5])
      (?range= dst-prev [-5 2])
      (?range= src [2 5])
      (?range= src-prev [-5 2]))))

;remove next option



