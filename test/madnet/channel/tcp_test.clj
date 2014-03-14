(ns madnet.channel.tcp-test
  (:require [khazad-dum :refer :all]
            [madnet.event :as e]
            [madnet.range :as r]
            [madnet.sequence :as s]
            [madnet.channel :as c]
            [madnet.channel.tcp :as t])
  (:import [madnet.channel ISocket]))

(defmacro with-tcp [[acceptor connector] & body]
  (let [acceptor-options (if (sequential? acceptor) (rest acceptor)
                             [:host "localhost" :port 12345])
        acceptor (if (sequential? acceptor) (first acceptor) acceptor)
        connector-options (if (sequential? connector) (rest connector)
                              [:host "localhost" :port 12345])
        connector (if (sequential? connector) (first connector)
                      connector)]
    `(with-open [~acceptor (t/bind ~@acceptor-options)
                 ~connector (t/connect ~@connector-options)]
       ~@body)))

(deftest simple-tcp-accept-connect-pair
  (with-tcp [acceptor connector]
    (with-open [ss (c/pop! acceptor :timeout 0)
                cs (c/pop! connector :timeout 0)]
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
  (?throws (c/pop! (t/connect :host "localhost" :port 12345))
           java.net.ConnectException))

(deftest reading-from-emtpy-acceptor-and-connector
  (with-open [acceptor (t/bind :host "localhost" :port 12345 :backlog 1)]
    (let [s (s/sequence 10)]
      (c/write! s acceptor)
      (?= (seq s) nil))
    (let [connectors (doall (repeatedly 5 #(t/connect :host "localhost"
                                                      :port 12345)))
          sockets (doall (map #(c/pop! % :timeout 0) connectors))]
      (?= (seq (first (c/write (s/sequence 10)
                               (t/connect :host "localhost"
                                          :port 12345)))) nil)
      (doall (map #(if % (.close %)) sockets))
      (doall (map #(.close %) connectors)))))

(defmacro ?socket [expr]
  `(with-open [socket# ~expr] 
     (?true (instance? ISocket socket#))))

(deftest writing-to-full-sequence
  (let [s (s/sequence 0)]
    (with-tcp [acceptor connector]
      (c/write! s acceptor)
      (?socket (c/pop! acceptor))
      (c/write! s connector)
      (?socket (c/pop! connector)))))

(deftest accept-several-times
  (with-tcp [acceptor connector]
    (with-open [connector (t/connect :host "localhost" :port 12345)
                connector (t/connect :host "localhost" :port 12345)
                connector (t/connect :host "localhost" :port 12345)
                connector (t/connect :host "localhost" :port 12345)
                connector (t/connect :host "localhost" :port 12345)
                connector (t/connect :host "localhost" :port 12345)]
      (?socket (c/pop! acceptor))
      (?socket (c/pop! acceptor))
      (let [s (s/sequence 1)]
        (c/write! s acceptor)
        (?socket (c/pop! s :timeout 0))
        (c/write! s acceptor)
        (?socket (c/pop! s :timeout 0)))
      (let [s (s/sequence 2)]
        (?= (c/write! s acceptor) (madnet.channel.Result. 2 2))
        (?socket (c/pop! s :timeout 0))
        (?socket (c/pop! s :timeout 0))))))

(deftest successful-connector-is-closed
  (with-tcp [acceptor connector]
    (?true (c/open? connector))
    (with-open [s (c/pop! connector)]
      (?false (c/open? connector))
      (?true (c/open? (.reader s)))
      (?true (c/open? (.writer s))))))

(deftest connector-and-acceptor-addresses
  (with-tcp [(acceptor :host "127.0.0.1" :port 12345) connector]
    (?= (t/address acceptor) {:host "localhost" :port 12345 :ip "127.0.0.1"})
    (?= (t/address connector) nil)
    (?= (t/remote-address acceptor) nil)
    (?= (t/remote-address connector) {:host "localhost" :port 12345 :ip "127.0.0.1"}))
  (with-tcp [(acceptor :host "::1" :port 12345)
             (connector :host "::1" :port 12345 :local-host "::1" :local-port 23456)]
    (?= (t/address acceptor) {:host "ip6-localhost" :port 12345 :ip "0:0:0:0:0:0:0:1"})
    (?= (t/address connector) {:host "ip6-localhost" :port 23456 :ip "0:0:0:0:0:0:0:1"})))

(deftest acceptor-and-connector-on-write-events
  (with-tcp [acceptor connector]
    (let [actions (atom [])]
      (with-open [s (e/event-set)
                  h1 (e/handler ([e s] (swap! actions conj :write1)) (-> acceptor .events .onWrite))
                  h2 (e/handler ([e s] (swap! actions conj :write2)) (-> connector .events .onWrite))]
        (c/register acceptor s)
        (c/register connector s)
        (e/do-selections [e s :timeout 0] (.handle e))
        (?= (set @actions) #{:write1 :write2})))))
      
      

      
