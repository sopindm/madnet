(ns madnet.event-test
  (:require [khazad-dum :refer :all]
            [madnet.event :as e])
  (:import [java.nio.channels ClosedSelectorException]))

(deftest making-simple-event
  (let [actions (atom [])
        e (e/event)
        h (e/handler ([e s] (swap! actions conj :src s :emitter e)) e)]
    (?= (seq (e/handlers e)) [h])
    (?= (seq (e/emitters h)) [e])
    (e/emit! e 123)
    (?= @actions [:src 123 :emitter e])))

(deftest closing-handler-during-iteration
  (letfn [(handler- [event]
            (e/handler ([e s] (.close this)) event))]
    (let [e (e/event)
          handlers (doall (repeatedly 100 #(handler- e)))]
      (e/emit! e 123)
      (?= (seq (e/handlers e)) nil)
      (?= (seq (e/emitters (first handlers))) nil))))

(deftest adding-handlers-during-iteration
  (letfn [(handler- [e] (e/handler ([e s] (handler- e)) e))]
    (let [e (e/event)
          handlers (doall (repeatedly 10 #(handler- e)))]
      (e/emit! e 123)
      (?= (count (e/handlers e)) 20))))

(deftest conj!-for-events
  (let [actions (atom [])
        e (e/event)
        h (e/handler ([e s]))]
    (e/conj! e h)
    (?= (seq (e/handlers e)) [h])
    (?= (seq (e/emitters h)) [e])))

(deftest disj!-for-events
  (let [e (e/event)
        h1 (e/handler ([e s]) e)
        h2 (e/handler ([e s]) e)]
    (e/disj! e h1 h2)
    (?= (seq (e/handlers e)) nil)
    (?= (seq (e/emitters h1)) nil)
    (?= (seq (e/emitters h2)) nil)))

(deftest adding-and-removing-in-iteration-correctness
  (let [e (e/event)
        handler (e/handler ([e s] (.popHandler e this)) e)]
    (e/emit! e 123)
    (?= (seq (e/emitters handler)) nil))
  (letfn [(handler- [e]
            (e/handler ([e s]
                          (.pushHandler e (handler- e))
                          (.close this)) e))]
    (let [e (e/event)
          h (handler- e)]
      (e/emit! e 123)
      (?= (seq (e/emitters (first (e/handlers e)))) [e]))))

(deftest removing-handler-without-iteration
  (let [e (e/event)
        handler (e/handler ([e s]) e)]
    (.popHandler e handler)
    (?= (seq (e/handlers e)) nil)
    (?= (seq (e/emitters handler)) nil)))

(deftest closing-events
  (let [e (e/event)
        h (e/handler ([e s]) e)]
    (.close e)
    (?= (seq (e/handlers e)) nil)
    (?= (seq (e/emitters h)) nil)))

(deftest closing-event-by-handler
  (letfn [(handler- [e] (e/handler ([e s] (.close e)) e))]
    (let [e (e/event)
          handlers (repeatedly 100 #(handler- e))]
      (e/emit! e 123)
      (?= (seq (e/handlers e)) nil))))

(deftest cannot-emit-emitting-event
  (let [e (e/event)
        handler (e/handler ([e s] (when (= s 123) (e/emit! e nil))) e)]
    (?throws (e/emit! e 123) UnsupportedOperationException
             "Cannot emit emitting event")))

(comment 
(deftest events-as-handlers
  (let [sources (atom [])
        e (e/event)
        h (e/event #(swap! sources conj :1 %2) e)]
    (e/emit! e 123)
    (?= (seq @sources) [:1 123])
    (e/emit! e 456)
    (?= (seq @sources) [:1 123 :1 456])))

(deftest one-shot-event
  (let [sources (atom [])
        e (e/event)
        h (e/event #(swap! sources conj %2) e :one-shot)]
    (e/emit! e 123)
    (?= @sources [123])
    (?= (seq (e/handlers e)) nil)
    (e/emit! e 123)
    (?= @sources [123]))))

;making persistent events
;transmitter event
;closing event
;emit multiple events

(comment
;;
;; Triggers
;;

(deftest simple-triggering
  (let [s (e/trigger-set)
        t (e/trigger)]
    (e/conj! s t)
    (e/start! t)
    (?= (seq (e/select s)) [t])))

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

(deftest canceling-trigger
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (e/cancel t1)
    (e/select s :timeout 0)
    (?= (set (e/signals s)) #{t2})
    (let [f (future (e/select s :timeout 4))]
      (Thread/sleep 2)
      (e/cancel t2)
      (?= (set @f) #{})
      (?= (set (e/signals s)) #{}))))

(deftest closing-trigger-provider
  (let [t (e/trigger)
        s (e/trigger-set t)]
    (e/start! t)
    (.close s)
    (?= (.provider t) nil)
    (?= (seq (e/signals s)) nil)
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
    (?= (e/attachment t) nil)
    (?= (seq (e/signals s)) nil)))

(deftest errors-adding-trigger
  (let [t (e/trigger)
        s (e/trigger-set)]
    (?throws (e/conj! s (proxy [Signal] [])) IllegalArgumentException)
    (?throws (e/conj! (proxy [SignalSet] [] (push [event] nil)) t)
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
    (?throws (e/conj! s (proxy [Signal] [])) IllegalArgumentException)
    (?throws (e/conj! (proxy [SignalSet] [] (push [event] nil)) t)
             IllegalArgumentException)))

;;
;; Timers
;;

(deftest making-simple-timer
  (let [t (e/timer 0)
        s (e/timer-set)]
    (e/conj! s t)
    (?= (seq (e/signals s)) [t])
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
    (?= (seq (e/signals s)) nil)
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
      (?= (seq (e/signals s)) nil))))

(deftest setting-timer-timeout
  (let [t (e/timer 123)]
    (?= (.timeout t) 123)
    (.setTimeout t 456)
    (?= (.timeout t) 456)))

(deftest closing-timer-provider
  (let [t (e/timer 123)
        s (e/timer-set t)]
    (e/start! t)
    (.close s)
    (?= (.provider t) nil)
    (?= (seq (e/signals s)) nil)
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
    (?= (e/attachment t) nil)
    (?= (.provider t) nil)
    (e/select s :timeout 0)
    (?= (seq (e/signals s)) nil)
    (?= (seq (.selections s)) nil)))

(deftest timer-attachment
  (let [t (e/timer 123 :some-attachment)]
    (?= (.timeout t) 123)
    (?= (e/attachment t) :some-attachment)))

(deftest errors-adding-timer
  (let [t (e/timer 123)
        s (e/timer-set)]
    (?throws (e/conj! s (proxy [Signal] [])) IllegalArgumentException)
    (?throws (e/conj! (proxy [SignalSet] [] (push [event] nil)) t)
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
     ~@body
     (.close ~source)
     (.close ~sink)))

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
    (e/start! re we)
    (.write writer (java.nio.ByteBuffer/wrap (byte-array (map byte (range 10)))))
    (e/cancel we)
    (?= (seq (e/select s)) [re])
    (?= (seq (e/signals s)) [re])
    (?= (.provider we) nil)))

(deftest closing-selector-set
  (with-pipe-events [reader writer re we s]
    (e/start! re we)
    (.close s)
    (?= (.provider re) nil)
    (?= (.provider we) nil)
    (?= (seq (e/signals s)) nil)
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
    (?throws (e/conj! s (proxy [Signal] [])) IllegalArgumentException)
    (?throws (e/conj! (proxy [SignalSet] [] (push [event] nil)) e)
             IllegalArgumentException)))

(deftest closing-event
  (let [[reader _] (pipe-)
        e (e/selector reader :read)
        s (e/selector-set e)]
    (e/attach! e 123)
    (.close e)
    (e/select s :timeout 0)
    (?= (.provider e) nil)
    (?= (e/attachment e) 123)
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

;;
;; Events multiset
;;

(defmacro with-events [[trigger [timer timeout] [reader writer] set] & body]
  `(let [~trigger (e/trigger)
         ~timer (e/timer ~timeout)
         pipe# (pipe-)
         [~'a-pipe-reader ~'a-pipe-writer] pipe#
         ~reader (e/selector (first pipe#) :read)
         ~writer (e/selector (second pipe#) :write)
         ~set (e/event-set ~trigger ~timer ~reader ~writer)]
     ~@body))

(deftest making-multiset
  (with-events [trigger [timer 123] [reader writer] s]
    (?= (set (e/signals s)) #{trigger timer reader writer})
    (?= (-> s .triggers e/signals seq) [trigger])
    (?= (-> s .timers e/signals seq) [timer])
    (?= (-> s .selectors e/signals set) #{reader writer})
    (?throws (e/conj! s (proxy [Signal] [])) IllegalArgumentException)))

(deftest selecting-on-multiset-with-trigger
  (with-events [trigger [timer 1000] [selector _] s]
    (e/start! trigger timer selector)
    (?= (e/for-selections [e s] e) [trigger])))

(deftest selecting-on-multiset-with-timer
  (with-events [trigger [timer 0] [selector _] s]
    (e/start! timer selector)
    (Thread/sleep 2)
    (?= (e/for-selections [e s] e) [timer]))
  (with-events [trigger [timer 3] [selector _] s]
    (e/start! timer selector)
    (?= (e/for-selections [e s] e) [timer]))
  (with-events [trigger [timer 5] [selector _] s]
    (e/start! timer selector)
    (let [f (future (e/select s))]
      (Thread/sleep 2)
      (e/start! trigger)
      (?= (seq @f) [trigger]))))

(deftest selecting-on-multiset-with-selector
  (with-events [trigger [timer 10] [reader writer] s]
    (e/start! timer reader writer)
    (?= (e/for-selections [e s] e) [writer]))
  (with-events [trigger [timer 10] [reader writer] s]
    (e/start! timer reader)
    (let [f (future (e/select s))]
      (Thread/sleep 3)
      (.write a-pipe-writer (java.nio.ByteBuffer/wrap (byte-array (map byte (range 10)))))
      (?= (seq @f) [reader])))
  (with-events [trigger [timer 10] [reader writer] s]
    (e/start! writer)
    (?= (seq (e/select s)) [writer])))

(deftest selecting-without-any-trigger
  (let [timer (e/timer 10)
        selector (e/selector (second (pipe-)) :write)
        s (e/event-set timer selector)]
    (e/start! selector timer)
    (?= (seq (e/select s)) [selector]))
  (let [selector (e/selector (second (pipe-)) :write)
        s (e/event-set selector)]
    (e/start! selector)
    (?= (seq (e/select s)) [selector])))

(deftest selecting-without-any-selector
  (let [timer (e/timer 10)
        trigger (e/trigger)
        s (e/event-set timer trigger)]
    (e/start! trigger timer)
    (?= (seq (e/select s)) [trigger]))
  (let [trigger (e/trigger)
        s (e/event-set trigger)]
    (e/start! trigger)
    (?= (seq (e/select s)) [trigger])))

(deftest selecting-event-set-now
  (with-events [trigger [timer 0] [reader writer] s]
    (e/start! trigger timer reader writer)
    (?= (set (e/select s :timeout 0)) #{trigger timer writer})))

(deftest selecting-multievent-with-timeout
  (with-events [trigger [timer 8] [reader writer] s]
    (e/start! timer reader)
    (?= (seq (e/select s :timeout 3)) nil)
    (e/start! trigger writer)
    (?= (set (e/for-selections [e s :timeout 3] e)) #{trigger writer})
    (e/stop! writer)
    (?= (set (e/for-selections [e s :timeout 10] e)) #{timer})))

(deftest selecting-multiset-with-only-timeouts
  (let [e (e/timer 4)
        s (e/event-set e)]
    (e/start! e)
    (?= (seq (e/for-selections [e s] e)) [e])
    (e/start! e)
    (?= (seq (e/for-selections [e s :timeout 0] e)) nil)
    (?= (seq (e/for-selections [e s :timeout 1] e)) nil)
    (?= (seq (e/for-selections [e s :timeout 10] e)) [e])))

(deftest closing-multiset
  (with-events [trigger [timer 0] [reader writer] s]
    (.close s)
    (?false (.isOpen s))
    (?false (.isOpen (.triggers s)))
    (?false (.isOpen (.timers s)))
    (?false (.isOpen (.selectors s)))))

(deftest for-selections-into
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/event-set t1 t2)]
    (e/start! t1 t2)
    (?= (e/for-selections [e s :into #{}]) #{t1 t2})))

;;
;; Handlers
;;

(defmacro with-handler [[handler expr & more-vars] & body]
  `(let [~'signal (e/trigger :attachment)
         ~handler ~expr
         ~@more-vars
         set# (e/event-set ~'signal)
         ~'signal! (fn []
                     (e/start! ~'signal)
                     (e/do-selections [event# set#] (e/handle! event#)))]
     ~@body))

(deftest handling-signals
  (let [a (atom [])]
    (with-handler [handler (reify IEventHandler
                             (onCallback [this event] (swap! a conj event)))]
      (.pushHandler signal handler)
      (signal!)
      (?= @a [:attachment])))
  (let [a (atom [])]
    (with-handler [handler (e/handler #(swap! a conj %) signal)]
      (signal!)
      (?= @a [:attachment])))
  (let [a (atom [])]
    (with-handler [handler (e/handler #(swap! a conj %) signal)]
      (let [handler (e/handler #(swap! a conj %) signal)]
        (signal!)
        (?= @a [:attachment :attachment])))))

(deftest simple-events
  (let [a (atom [])]
    (with-handler [event (e/event #(swap! a conj %) signal)]
      (signal!)
      (?= @a [:attachment])))
  (let [a (atom [])]
    (with-handler [event (e/event identity signal)
                   event2 (e/event #(swap! a conj %) event)]
      (signal!)
      (?= @a [:attachment]))))

(deftest or-event-test
  (let [a (atom [])]
    (with-handler [event (e/event identity signal)
                   event1 (e/event identity event)
                   event2 (e/event identity event)
                   or-event (e/event #(swap! a conj %) event1 event2)]
      (signal!)
      (?= @a [:attachment :attachment]))))

(deftest timer-signal-events
  (let [a (atom [])
        t (e/timer 0 :something)
        s (e/event-set t)
        event (e/event #(swap! a conj %) t)]
    (e/start! t)
    (e/do-selections [e s] (e/handle! e))
    (?= @a [:something])))

(deftest selector-signal-events
  (let [a (atom [])
        signal (e/selector (second (pipe-)) :write :something)
        s (e/event-set signal)
        event (e/event #(swap! a conj %) signal)]
    (e/start! signal)
    (e/do-selections [e s] (e/handle! e))
    (?= @a [:something])))

;flash signal

(deftest making-flash-signal
  (let [a (atom [])
        signal (e/flash :something)
        event (e/event #(swap! a conj %) signal)]
    (e/start! signal)
    (?= @a [:something])))

(deftest flash-signal-attachment
  (?= (e/attachment (e/flash 42)) 42)
  (let [signal (e/flash)]
    (e/attach! signal 4321)
    (?= (e/attachment signal) 4321)))

(deftest closing-flash-signal
  (let [signal (e/flash 123)]
    (.close signal)
    (?= (e/attachment signal) nil)))

(deftest cannot-register-flash-signal
  (let [signal (e/flash)]
    (?throws (e/conj! (proxy [SignalSet] [] (push [event] nil)) signal)
             UnsupportedOperationException)))

;;
;; Event loops
;;

(deftest event-loops
  (let [a (atom [])
        t1 (e/trigger 1)
        t2 (e/trigger 2)
        s (e/event-set t1 t2)
        e1 (e/event #(swap! a conj %) t1)
        e2 (e/event #(swap! a conj %) t2)
        f (future (e/loop s))]
    (e/start! t1)
    (Thread/sleep 1)
    (?= @a [1])
    (reset! a [])
    (e/start! t1 t2)
    (Thread/sleep 1)
    (?= (set @a) #{1 2})
    (future-cancel f))))
