(ns madnet.range.ProxyRange-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c]
            [madnet.range :as r]
            [madnet.range-test :use [irange srange]])
  (:import [madnet.range IntegerRange]
           [madnet.channel Result]))

(deftest range-proxy-making
  (let [r (irange 0 10)
        p (r/proxy r)]
    (?= (r/size p) 10)
    (?range= (.range p) [0 10])))
    
(deftest range-proxy-cloning
  (let [r (irange 0 10)
        pr (r/proxy r)
        prc (.clone pr)]
    (?range= (.range prc) [0 10])
    (r/drop! 5 r)
    (?range= (.range prc) [0 10])))

(deftest closing-range-proxy
  (let [r (irange 0 10)
        pr (r/proxy r)]
    (?true (c/open? pr))))

(deftest range-proxy-operations
  (let [pr (r/proxy (irange 0 10))]
    (?= (r/size (r/drop! 3 pr)) 7)
    (?range= (.range pr) [3 10])
    (?= (r/size (r/take! 5 pr)) 5)
    (?range= (.range pr) [3 8])
    (?= (r/size (r/expand! 3 pr)) 8)
    (?range= (.range pr) [3 11])
    (?= (r/size (r/take-last! 4 pr)) 4)
    (?range= (.range pr) [7 11])
    (?= (r/size (r/drop-last! 1 pr)) 3)
    (?range= (.range pr) [7 10])))

(deftest range-proxy-read-and-write
  (let [writeable (proxy [IntegerRange] [0 10]
                    (write [seq] (throw (UnsupportedOperationException.
                                         "Writing to proxies is OK"))))
        readable (proxy [IntegerRange] [0 10]
                   (read [seq] (throw (UnsupportedOperationException.
                                       "Reading from proxies is OK"))))
        wp (r/proxy writeable)
        rp (r/proxy readable)
        range (IntegerRange. 0 10)]
    (?throws (.write wp range) UnsupportedOperationException
             "Writing to proxies is OK")
    (?throws (.write rp readable) UnsupportedOperationException
             "Reading from proxies is OK")
    (?throws (.read rp readable) UnsupportedOperationException
             "Reading from proxies is OK")
    (?throws (.read wp writeable) UnsupportedOperationException
             "Writing to proxies is OK")))

(deftest read-only-and-write-only-proxies
  (let [r (proxy [IntegerRange] [0 10]
            (write [seq] (Result. 0 0))
            (read [seq] (Result. 1 1)))
        rp (r/proxy r :read-only)
        wp (r/proxy r :write-only)]
    (?= (c/read! rp wp) (Result. 1 1))
    (?throws (c/read! wp rp) UnsupportedOperationException)
    (?= (c/write! wp rp) (Result. 0 0))
    (?throws (c/write! rp wp) UnsupportedOperationException)))

(deftest extending-range-proxy
  (let [r (irange 0 10)
        p (r/proxy r (expand [n] this))]
    (?range= (.range ^madnet.range.ProxyRange (r/expand 1000 p)) [0 10])))

(deftest iterating-proxy-range
  (let [r (srange 0 10 (range 10))
        pr (r/proxy r)]
    (?= (seq pr) (seq (range 10)))))





