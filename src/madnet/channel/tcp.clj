(ns madnet.channel.tcp
  (:import [java.net InetSocketAddress]
           [java.nio.channels SocketChannel ServerSocketChannel]
           [madnet.channel.tcp AcceptChannel ConnectChannel]))

(defn bind [& {:keys [host port backlog reuse-address receive-buffer]
               :as options}]
  (let [listener (ServerSocketChannel/open)]
    (when reuse-address
      (.setOption listener
                  java.net.StandardSocketOptions/SO_REUSEADDR true))
    (when receive-buffer
      (.setOption listener
                  java.net.StandardSocketOptions/SO_RCVBUF
                  (int receive-buffer)))
    (if backlog
      (.bind listener (java.net.InetSocketAddress. host (int port))
             (int backlog))
      (.bind listener (java.net.InetSocketAddress. host (int port))))
    (AcceptChannel. listener)))

(defn connect [& {:keys [host port local-host local-port] :as options}]
  (let [channel (SocketChannel/open)]
    (when (and local-host local-port)
      (.bind channel (java.net.InetSocketAddress. local-host local-port)))
    (ConnectChannel. channel (java.net.InetSocketAddress. host port))))

(defn- inet-address->map [address]
  {:host (.getHostName address)
   :port (.getPort address)
   :ip (-> address .getAddress .getHostAddress)})

(defn- address->map [address]
  (if (instance? InetSocketAddress address) (inet-address->map address) {}))

(defn address [channel]
  (if-let [address (-> channel .channel .getLocalAddress)] (address->map address)))

(defn remote-address [channel]
  (let [channel (.channel channel)]
    (if (instance? SocketChannel channel)
      (if-let [address (.getRemoteAddress channel)] (address->map address) {}))))





  
  
