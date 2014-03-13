(ns madnet.channel.tcp-test
  (:require [khazad-dum :refer :all]
            [madnet.sequence :as s]
            [madnet.channel :as c]
            [madnet.channel.tcp :as t])
  (:import [madnet.channel ISocket]))

(defmacro with-tcp [[acceptor connector] & body]
  `(with-open [~acceptor (t/bind :host "localhost" :port 12345)
               ~connector (t/connect :host "localhost" :port 12345)]
     ~@body))

(deftest simple-tcp-accept-connect-pair
  (with-tcp [acceptor connector]
    (with-open [ss (c/pop! acceptor)
                cs (c/pop! connector)]
      (?true (instance? ISocket ss))
      (?true (instance? ISocket cs)))))

(deftest cannot-write-to-acceptor-and-connector
  (with-tcp [acceptor connector]
    (?throws (c/push! acceptor 123) UnsupportedOperationException)
    (?throws (c/write! acceptor (s/sequence 10))
             UnsupportedOperationException)
    (?throws (c/push! connector 123) UnsupportedOperationException)
    (?throws (c/write! connector (s/sequence 10))
             UnsupportedOperationException)))

(deftest writing-from-acceptor-and-connector
  (with-tcp [acceptor connector]
    (let [s1 (s/sequence 1)
          s2 (s/sequence 1)]
      (c/write! s1 acceptor)
      (c/write! s2 connector)
      (with-open [server (c/pop! s1)
                  client (c/pop! s2)]
        (?true (instance? ISocket server))
        (?true (instance? ISocket client))))))

;writing to full sequence from acceptor
;popping twice from acceptor (and after read)

;writing from acceptor and connector to object range
;successfull connector is closed
;accept several times
;closing acceptor and connector (try reuse address)
;acceptor backlog and reuse_address
;connector local address

;getting connector and acceptor addresses

;acceptor/connector with wildcard
;acceptor/connector with ipv6

;pop from emptyh acceptor and connector

;acceptor and connector on-write event
      
      

      
