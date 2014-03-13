(ns madnet.channel.tcp-test
  (:require [khazad-dum :refer :all]
            [madnet.sequence :as s]
            [madnet.channel :as c]
            [madnet.channel.tcp :as t])
  (:import [madnet.channel ISocket]))

(defmacro with-tcp [[acceptor connector] & body]
  (let [acceptor-options (if (sequential? acceptor) (rest acceptor) [:host "localhost" :port 12345])
        acceptor (if (sequential? acceptor) (first acceptor) acceptor)
        connector-options (if (sequential? connector) (rest connector) [:host "localhost" :port 12345])
        connector (if (sequential? connector) (first connector) connector)]
    `(with-open [~acceptor (t/bind ~@acceptor-options)
                 ~connector (t/connect ~@connector-options)]
       ~@body)))

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

(deftest popping-from-empty-acceptor-and-connector
  (with-open [acceptor (t/bind :host "localhost" :port 12345 :backlog 1)]
    (?= (c/pop! acceptor :timeout 0) nil)
    (let [connectors (doall (repeatedly 10 #(t/connect :host "localhost" :port 12345)))
          sockets (doall (map #(c/pop! % :timeout 0) connectors))]
      (?true (> (count (filter nil? sockets)) 0))
      (doall (map #(if % (.close %)) sockets))
      (doall (map #(.close %) connectors)))))

(deftest connecting-to-wrong-address
  (?throws (c/pop! (t/connect :host "localhost" :port 12345)) java.net.ConnectException))

(deftest reading-from-emtpy-acceptor-and-connector
  (with-open [acceptor (t/bind :host "localhost" :port 12345 :backlog 1)]
    (let [s (s/sequence 10)]
      (c/write! s acceptor)
      (?= (seq s) nil))
    (let [connectors (doall (repeatedly 5 #(t/connect :host "localhost" :port 12345)))
          sockets (doall (map #(c/pop! % :timeout 0) connectors))]
      (?= (seq (first (c/write (s/sequence 10) (t/connect :host "localhost" :port 12345)))) nil)
      (doall (map #(if % (.close %)) sockets))
      (doall (map #(.close %) connectors)))))

;writing to full sequence from acceptor
;writing several sockets from acceptor
;popping twice from acceptor (and after read)

;successfull connector is closed
;accept several times
;closing acceptor and connector (try reuse address)
;acceptor backlog and reuse_address
;connector local address

;getting connector and acceptor addresses

;acceptor/connector with wildcard
;acceptor/connector with ipv6

;acceptor and connector on-write event
      
      

      
