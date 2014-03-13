(ns madnet.channel.tcp
  (:import [java.nio.channels SocketChannel ServerSocketChannel]
           [madnet.channel.tcp AcceptChannel ConnectChannel]))

(defn bind [& {:keys [address port backlog] :as options}]
  (let [listener (ServerSocketChannel/open)]
    (if backlog
      (.bind listener (java.net.InetSocketAddress. address (int port)) (int backlog))
      (.bind listener (java.net.InetSocketAddress. address (int port))))
    (AcceptChannel. listener)))

(defn connect [& {:keys [address port] :as options}]
  (let [channel (SocketChannel/open)]
    (ConnectChannel. channel (java.net.InetSocketAddress. address port))))
  
  
  
