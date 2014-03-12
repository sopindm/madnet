(ns madnet.channel.tcp-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c]
            [madnet.channel.tcp :as t])
  (:import [madnet.channel ReadableChannel WritableChannel]))

(defmacro with-open-socket [[[reader writer] form] & body]
  `(let [[~reader ~writer] ~form]
     (with-open [~reader ~reader ~writer ~writer] ~@body)))

(defmacro with-open-sockets [[& bindings] & body]
  (if (empty? bindings) `(do ~@body)
      `(with-open-socket [~(first bindings) ~(second bindings)]
         (with-open-sockets [~@(drop 2 bindings)] ~@body))))

(deftest simple-tcp-accept-connect-pair
  (with-open [acceptor (t/bind :host "localhost" :port 12345)
              connector (t/connect :host "localhost" :port 12345)]
    (with-open-sockets [[sreader swriter] (c/pop! acceptor)
                        [creader cwriter] (c/pop! connector)]
      (?true (instance? ReadableChannel sreader))
      (?true (instance? ReadableChannel creader))
      (?true (instance? WritableChannel swriter))
      (?true (instance? WritableChannel cwriter)))))

;cannot read from acceptor and connector
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
      
      

      
