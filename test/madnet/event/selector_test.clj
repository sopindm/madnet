(ns madnet.event.selector-test
  (:require [khazad-dum :refer :all]
            [madnet.event :as e]
            [madnet.event-test :refer [pipe-]])
  (:import [java.nio.channels ClosedSelectorException]))

(defmacro with-pipe-events [[source sink reader writer set] & body]
  `(let [[~source ~sink] (pipe-)
         ~reader (e/selector ~source :read)
         ~writer (e/selector ~sink :write)
         ~set (e/selector-set ~reader ~writer)]
     ~@body
     (.close ~source)
     (.close ~sink)))

(deftest making-selector
  (with-pipe-events [sr sw reader writer s]
    (e/emit! reader)
    (e/emit! writer)
    (?= (seq (e/select s)) [writer])
    (?= (.provider reader) s))
  (with-pipe-events [reader writer re we s]
    (e/emit! re)
    (e/emit! we)
    (.write writer (java.nio.ByteBuffer/wrap (byte-array (map byte [1 2 3]))))
    (?= (set (e/select s)) #{re we}))
  (?throws (e/selector (.source (java.nio.channels.Pipe/open)) :write) IllegalArgumentException)
  (?throws (e/selector (.sink (java.nio.channels.Pipe/open)) :read) IllegalArgumentException))

(deftest registering-select-event-twice
  (let [e (e/selector (first (pipe-)) :read)]
    (e/conj! (e/selector-set) e)
    (?throws (e/conj! (e/selector-set) e) IllegalArgumentException)))

(deftest selectors-with-timeout
  (?= (seq (e/select (e/selector-set) :timeout 0)) nil)
  (let [f (future (e/select (e/selector-set) :timeout 4))]
    (Thread/sleep 2)
    (?false (realized? f))
    (Thread/sleep 4)
    (?true (realized? f))))

(deftest interrupting-selector
  (let [s (e/selector-set)
        f (future (e/select s))]
    (Thread/sleep 2)
    (?false (realized? f))
    (.interrupt s)
    (Thread/sleep 1)
    (?true (realized? f))))

(deftest canceling-selector-event
  (with-pipe-events [reader writer re we s]
    (e/emit! re)
    (e/emit! we)
    (.write writer (java.nio.ByteBuffer/wrap (byte-array (map byte (range 10)))))
    (e/cancel! we)
    (?= (seq (e/select s)) [re])
    (?= (seq (e/signals s)) [re])
    (?= (.provider we) nil)))

(deftest closing-selector-set
  (with-pipe-events [reader writer re we s]
    (e/emit! re)
    (e/emit! we)
    (.close s)
    (?= (.provider re) nil)
    (?= (.provider we) nil)
    (?= (seq (e/signals s)) nil)
    (?= (seq (.selections s)) nil)
    (?throws (e/select s) ClosedSelectorException)
    (?throws (e/select s :timeout 0) ClosedSelectorException)
    (?throws (e/select s :timeout 10) ClosedSelectorException)
    (?throws (e/conj! s we) ClosedSelectorException)))

(deftest errors-adding-selector
  (let [e (e/selector (first (pipe-)) :read)
        s (e/selector-set)]
    (?throws (e/conj! s (proxy [madnet.event.Signal] [])) IllegalArgumentException)
    (?throws (e/conj! (proxy [madnet.event.SignalSet] [] (conj [event] (.register event this))) e)
             IllegalArgumentException)))

(deftest closing-event
  (let [[reader _] (pipe-)
        e (e/selector reader :read)
        s (e/selector-set e)]
    (.close e)
    (e/select s :timeout 0)
    (?= (.provider e) nil)
    (?= (seq (e/signals s)) nil)))

(deftest closing-selector-closes-event
  (let [[reader _] (pipe-)
        re (e/selector reader :read)
        e (e/event)
        h (e/handler () re)]
    (e/attach! re 123)
    (e/conj! e re)
    (.close re)
    (?= (e/attachment re) nil)
    (?= (seq (e/handlers e)) nil)
    (?= (seq (e/emitters h)) nil)))

(deftest handling-selector
  (let [e (e/selector (first (pipe-)) :read)
        actions (atom [])
        h (e/handler ([e s] (swap! actions conj {:emit e :src s})) e)]
    (e/attach! e 123)
    (.handle e)
    (?= (seq @actions) [{:emit e :src 123}])
    (.handle e 456)
    (?= (seq @actions) [{:emit e :src 123} {:emit e :src 456}])))

(deftest disj-for-selectors
  (let [e (e/selector (first (pipe-)) :read)
        s (e/selector-set e)]
    (e/disj! s e)
    (e/select s :timeout 0)
    (?= (.provider e) nil)
    (?= (seq (e/signals s)) nil)))

(deftest accept-event
  (let [accept-ch (java.nio.channels.ServerSocketChannel/open)]
    (e/selector accept-ch :accept)
    (?throws (e/selector accept-ch :read) IllegalArgumentException)
    (?throws (e/selector accept-ch :write) IllegalArgumentException)
    (?throws (e/selector accept-ch :connect) IllegalArgumentException))
  (let [[reader writer] (pipe-)]
    (?throws (e/selector reader :accept) IllegalArgumentException)
    (?throws (e/selector reader :connect) IllegalArgumentException)
    (?throws (e/selector writer :accept) IllegalArgumentException)
    (?throws (e/selector writer :connect) IllegalArgumentException))
  (let [connect-ch (java.nio.channels.SocketChannel/open)]
    (e/selector connect-ch :connect)
    (e/selector connect-ch :read)
    (e/selector connect-ch :write)
    (?throws (e/selector connect-ch :accept) IllegalArgumentException)))

(deftest making-no-persistent-selector
  (let [e (e/selector (second (pipe-)) :write)
        s (e/selector-set e)]
    (e/set-persistent! e false)
    (?false (e/persistent? e))
    (e/emit! e)
    (?= (e/for-selections [e s] (.handle e) e) [e])
    (?= (seq (e/select s :timeout 0)) nil)))
