(ns madnet.event-test
  (:require [khazad-dum :refer :all]
            [madnet.event :as e])
  (:import [java.nio.channels ClosedSelectorException]
           [madnet.event Event EventSet]))

;;
;; Triggers
;;

(deftest simple-triggering
  (let [s (e/trigger-set)
        t (e/trigger)]
    (e/conj! s t)
    (e/start! t)
    (?= (seq (e/select s)) [t])))

(deftest touching-no-registered-trigger
  (?throws (e/start! (e/trigger)) NullPointerException))

(deftest touching-trigger-twice-has-no-effect
  (let [t (e/trigger)
        s (e/trigger-set t)]
    (e/start! t)
    (e/start! t)
    (?= (seq (e/select s)) [t])))

(deftest touching-several-triggers
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (dorun (map e/start! [t1 t2]))
    (?= (set (e/select s)) #{t1 t2})))

(deftest touching-set-with-several-triggers
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (e/start! t1)
    (?= (seq (e/select s)) [t1])))

(deftest select!-is-blocking-for-triggers
  (let [t (e/trigger)
        s (e/trigger-set t)
        f (future (e/select s))]
    (Thread/sleep 10)
    (?false (realized? f))
    (e/start! t)
    (let [a (agent f)]
      (send-off a #(deref %))
      (when (not (await-for 1000 a)) (throw (RuntimeException. "Agent timeout")))
      (?= (seq @a) [t]))))

(deftest do-selections-test
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (e/start! t1)
    (e/start! t2)
    (let [a (atom [])]
      (e/do-selections [e s]
        (swap! a conj e))
      (?= (set @a) #{t1 t2})
      (?= (seq (.selections s)) nil))))

(deftest for-selections-test
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (e/start! t1 t2)
    (?= (set (e/for-selections [e s] e))
        #{t1 t2})))

(deftest registering-trigger-in-multiple-sets-error
  (let [t (e/trigger)]
    (e/conj! (e/trigger-set) t)
    (?throws (e/conj! (e/trigger-set) t) IllegalArgumentException)))

(deftest triggers-attachment
  (let [t (e/trigger 42)]
    (?= (e/attachment t) 42))
  (let [t (e/trigger)]
    (e/attach! t 123)
    (?= (e/attachment t) 123)))
    
(deftest selecting-tirgger-now
  (let [s (e/trigger-set)]
    (?= (seq (e/select s :timeout 0)) nil)
    (let [t (e/trigger)]
      (e/conj! s t)
      (e/start! t)
      (?= (seq (e/select s :timeout 0)) [t]))))

(deftest selecting-trigger-with-timeout
  (let [s (e/trigger-set (e/trigger))
        f (future (e/select s :timeout 3))]
    (Thread/sleep 1)
    (?false (realized? f))
    (Thread/sleep 6)
    (?true (realized? f))))

(deftest interrupting-trigger-selection
  (let [s (e/trigger-set (e/trigger))
        f (future (e/select s))]
    (Thread/sleep 5)
    (?false (realized? f))
    (e/interrupt s)
    (Thread/sleep 2)
    (?true (realized? f))
    (?= (seq (.selections s)) nil)))
    
(deftest single-trigger-interrupt-does-not-interrupt-two-selections
  (let [s (e/trigger-set (e/trigger))
        f (future (e/select s)
                  (e/select s))]
    (Thread/sleep 2)
    (e/interrupt s)
    (Thread/sleep 2)
    (?false (realized? f))
    (e/interrupt s)
    (Thread/sleep 2)
    (?true (realized? f))))

(deftest canceling-trigger
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (e/cancel t1)
    (e/select s :timeout 0)
    (?= (set (e/events s)) #{t2})
    (let [f (future (e/select s :timeout 4))]
      (Thread/sleep 2)
      (e/cancel t2)
      (?= (set @f) #{})
      (?= (set (e/events s)) #{}))))

(deftest closing-trigger-provider
  (let [t (e/trigger)
        s (e/trigger-set t)]
    (e/start! t)
    (.close s)
    (?= (.provider t) nil)
    (?= (seq (e/events s)) nil)
    (?= (seq (.selections s)) nil)
    (?throws (e/select s) ClosedSelectorException)
    (?throws (e/select s :timeout 0) ClosedSelectorException)
    (?throws (e/select s :timeout 10) ClosedSelectorException)
    (?throws (e/conj! s t) ClosedSelectorException)))

(deftest closing-tirgger-event
  (let [t (e/trigger 123)
        s (e/trigger-set t)]
    (?= (.provider t) s)
    (e/start! t)
    (.close t)
    (?= (.provider t) nil)
    (e/select s :timeout 0)
    (?= (seq (e/events s)) nil)
    (?= (seq (.selections s)) nil)
    (?= (e/attachment t) nil)))

(deftest errors-adding-trigger
  (let [t (e/trigger)
        s (e/trigger-set)]
    (?throws (e/conj! s (proxy [Event] [])) IllegalArgumentException)
    (?throws (e/conj! (proxy [EventSet] [] (push [event] nil)) t)
             IllegalArgumentException)))

(deftest trigger-set-without-events-doesnt-block
  (let [s (e/trigger-set)]
    (?= (seq (e/select s)) nil)
    (?= (seq (e/select s :timeout 1000000)) nil)))

(deftest trigger-set-without-events-doesnt-block
  (let [s (e/trigger-set)]
    (?= (seq (e/select s)) nil)
    (?= (seq (e/select s :timeout 1000000)) nil)))

(deftest errors-adding-trigger
  (let [t (e/trigger)
        s (e/trigger-set)]
    (?throws (e/conj! s (proxy [Event] [])) IllegalArgumentException)
    (?throws (e/conj! (proxy [EventSet] [] (push [event] nil)) t)
             IllegalArgumentException)))

;;
;; Timers
;;

(deftest making-simple-timer
  (let [t (e/timer 0)
        s (e/timer-set)]
    (e/conj! s t)
    (?= (seq (e/events s)) [t])
    (e/start! t)
    (?= (seq (e/select s)) [t])))
    
(deftest making-timer-with-timeout
  (let [t (e/timer 3)
        s (e/timer-set t)]
    (e/start! t)
    (?= (seq (e/select s :timeout 0)) nil)
    (Thread/sleep 4)
    (?= (seq (e/select s :timeout 0)) [t])))

(deftest timer-blocking-select
  (let [t (e/timer 2)
        s (e/timer-set t)]
    (e/start! t)
    (?= (seq (e/select s)) [t])))
    
(deftest select-with-no-active-timers-doesnt-block
  (?= (seq (e/select (e/timer-set))) nil)
  (let [t (e/timer 10)
        s (e/timer-set t)]
    (?= (seq (e/select s)) nil)
    (?= (seq (e/select s :timeout 1000000)) nil)))

(deftest two-events-with-same-timestamp
  (let [[t1 t2] (repeatedly 2 #(e/timer 1))
        s (e/timer-set t1 t2)]
    (doall (map e/start! [t1 t2]))
    (?= (set (e/select s)) #{t1 t2})))

(deftest starting-no-registered-timer
  (?throws (e/start! (e/timer 1)) NullPointerException))

(deftest stopping-timer
  (let [t (e/timer 1)
        s (e/timer-set t)]
    (e/start! t)
    (e/stop! t)
    (?= (seq (e/select s)) nil))
  (let [[t1 t2] (repeatedly 2 #(e/timer 1))
        s (e/timer-set t1 t2)]
    (doall (map e/start! [t1 t2]))
    (e/stop! t1)
    (?= (seq (e/select s)) [t2])))

(deftest selecting-timer-with-timeout
  (let [t (e/timer 5)
        s (e/timer-set t)]
    (e/start! t)
    (?= (seq (e/select s :timeout 3)) nil)
    (?= (seq (e/select s :timeout 3)) [t])))

(deftest canceling-timer
  (let [t (e/timer 0)
        s (e/timer-set t)]
    (e/cancel t)
    (?= (seq (e/select s)) nil)
    (?= (.provider t) nil)
    (?= (seq (e/events s)) nil)
    (?= (seq (.selections s)) nil))
  (let [t (e/timer 4)
        t2 (e/timer 5)
        s (e/timer-set t t2)]
    (e/start! t)
    (e/start! t2)
    (e/cancel t)
    (?= (seq (e/select s)) [t2]))
  (let [t (e/timer 5)
        s (e/timer-set t)]
    (e/start! t)
    (let [f (future (e/select s :timeout 5))]
      (Thread/sleep 2)
      (e/cancel t)
      (?= (seq @f) nil)
      (?= (seq (e/events s)) nil))))

(deftest setting-timer-timeout
  (let [t (e/timer 123)]
    (?= (.timeout t) 123)
    (.setTimeout t 456)
    (?= (.timeout t) 456)))

(deftest timer-attachment
  (let [t (e/timer 123 :some-attachment)]
    (?= (.timeout t) 123)
    (?= (e/attachment t) :some-attachment)))

(deftest interrupting-timer
  (letfn [(?interrupt [set future]
            (?false (realized? future))
            (e/interrupt set)
            (Thread/sleep 1)
            (?true (realized? future))
            (?= (seq (.selections set)) nil))]
    (let [s (e/timer-set (e/timer 100) (e/timer 500))]
      (?interrupt s (future (e/select s))))
    (let [s (e/timer-set (e/timer 100) (e/timer 100500))]
      (?interrupt s (future (e/select s :timeout 100))))))

(deftest closing-timer-provider
  (let [t (e/timer 123)
        s (e/timer-set t)]
    (e/start! t)
    (.close s)
    (?= (.provider t) nil)
    (?= (seq (e/events s)) nil)
    (?= (seq (.selections s)) nil)
    (?throws (e/select s) ClosedSelectorException)
    (?throws (e/select s :timeout 0) ClosedSelectorException)
    (?throws (e/select s :timeout 10) ClosedSelectorException)
    (?throws (e/conj! s t) ClosedSelectorException)))

(deftest closing-timer
  (let [t (e/timer 123)
        s (e/timer-set t)]
    (e/start! t)
    (.close t)
    (?= (.provider t) nil)
    (e/select s :timeout 0)
    (?= (seq (e/events s)) nil)
    (?= (seq (.selections s)) nil)
    (?= (e/attachment t) nil)))

(deftest errors-adding-timer
  (let [t (e/timer 123)
        s (e/timer-set)]
    (?throws (e/conj! s (proxy [Event] [])) IllegalArgumentException)
    (?throws (e/conj! (proxy [EventSet] [] (push [event] nil)) t)
             IllegalArgumentException)))

;;
;; Selectors
;;

(defn pipe- []
  (let [pipe (java.nio.channels.Pipe/open)]
    (.configureBlocking (.sink pipe) false)
    (.configureBlocking (.source pipe) false)
    [(.source pipe) (.sink pipe)]))

(defmacro with-pipe-events [[source sink reader writer set] & body]
  `(let [[~source ~sink] (pipe-)
         ~reader (e/selector ~source :read)
         ~writer (e/selector ~sink :write)
         ~set (e/selector-set ~reader ~writer)]
     ~@body))

(deftest making-selector
  (with-pipe-events [sr sw reader writer s]
    (e/start! reader writer)
    (?= (seq (e/select s)) [writer])
    (?= (.provider reader) s))
  (with-pipe-events [reader writer re we s]
    (e/start! re we)
    (.write writer (java.nio.ByteBuffer/wrap (byte-array (map byte [1 2 3]))))
    (?= (set (e/select s)) #{re we}))
  (?throws (e/selector (.source (java.nio.channels.Pipe/open)) :write) IllegalArgumentException)
  (?throws (e/selector (.sink (java.nio.channels.Pipe/open)) :read) IllegalArgumentException))

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
    (e/interrupt s)
    (Thread/sleep 1)
    (?true (realized? f))))

(deftest canceling-selector-event
  (with-pipe-events [reader writer re we s]
    (e/start! re we)
    (.write writer (java.nio.ByteBuffer/wrap (byte-array (map byte (range 10)))))
    (e/cancel we)
    (?= (seq (e/select s)) [re])
    (?= (seq (e/events s)) [re])
    (?= (.provider we) nil)))

(deftest closing-selector-set
  (with-pipe-events [reader writer re we s]
    (e/start! re we)
    (.close s)
    (?= (.provider re) nil)
    (?= (.provider we) nil)
    (?= (seq (e/events s)) nil)
    (?= (seq (.selections s)) nil)
    (?throws (e/select s) ClosedSelectorException)
    (?throws (e/select s :timeout 0) ClosedSelectorException)
    (?throws (e/select s :timeout 10) ClosedSelectorException)
    (?throws (e/conj! s we) ClosedSelectorException)))

(deftest selector-attachments
  (let [[reader writer] (pipe-)
        e (e/selector reader :read)]
    (?= (e/attachment e) nil)
    (e/attach! e 123)
    (?= (e/attachment e) 123)))

(deftest errors-adding-selector
  (let [e (e/selector (first (pipe-)) :read)
        s (e/selector-set)]
    (?throws (e/conj! s (proxy [Event] [])) IllegalArgumentException)
    (?throws (e/conj! (proxy [EventSet] [] (push [event] nil)) e)
             IllegalArgumentException)))

(deftest closing-event
  (let [[reader _] (pipe-)
        e (e/selector reader :read)
        s (e/selector-set e)]
    (e/attach! e 123)
    (.close e)
    (e/select s :timeout 0)
    (?= (e/attachment e) nil)
    (?= (.provider e) nil)
    (?= (seq (e/events s)) nil)))

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


