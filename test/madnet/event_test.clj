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
    (e/touch! t)
    (?= (seq (e/select s)) [t])))

(deftest touching-no-registered-trigger
  (?throws (e/touch! (e/trigger)) NullPointerException))

(deftest touching-trigger-twice-has-no-effect
  (let [t (e/trigger)
        s (e/trigger-set t)]
    (e/touch! t)
    (e/touch! t)
    (?= (seq (e/select s)) [t])))

(deftest touching-several-triggers
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (dorun (map e/touch! [t1 t2]))
    (?= (set (e/select s)) #{t1 t2})))

(deftest touching-set-with-several-triggers
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (e/touch! t1)
    (?= (seq (e/select s)) [t1])))

(deftest select!-is-blocking
  (let [t (e/trigger)
        s (e/trigger-set t)
        f (future (e/select s))]
    (Thread/sleep 10)
    (?false (realized? f))
    (e/touch! t)
    (let [a (agent f)]
      (send-off a #(deref %))
      (when (not (await-for 1000 a)) (throw (RuntimeException. "Agent timeout")))
      (?= (seq @a) [t]))))

(deftest do-selections-test
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (e/touch! t1)
    (e/touch! t2)
    (let [a (atom [])]
      (e/do-selections [e s]
        (swap! a conj e))
      (?= (set @a) #{t1 t2})
      (?= (seq (.selections s)) nil))))

(deftest for-selections-test
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (e/touch! t1 t2)
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
    
(deftest selecting-now
  (let [s (e/trigger-set)]
    (?= (seq (e/select s :timeout 0)) nil)
    (let [t (e/trigger)]
      (e/conj! s t)
      (e/touch! t)
      (?= (seq (e/select s :timeout 0)) [t]))))

(deftest selecting-with-timeout
  (let [s (e/trigger-set (e/trigger))
        f (future (e/select s :timeout 3))]
    (Thread/sleep 1)
    (?false (realized? f))
    (Thread/sleep 6)
    (?true (realized? f))))

(deftest interrupting-selection
  (let [s (e/trigger-set (e/trigger))
        f (future (e/select s))]
    (Thread/sleep 5)
    (?false (realized? f))
    (e/interrupt s)
    (Thread/sleep 2)
    (?true (realized? f))
    (?= (seq (.selections s)) nil)))
    
(deftest single-interrupt-does-not-interrupt-two-selections
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

(deftest closing-provider
  (let [t (e/trigger)
        s (e/trigger-set t)]
    (e/touch! t)
    (.close s)
    (?= (.provider t) nil)
    (?= (seq (e/events s)) nil)
    (?= (seq (.selections s)) nil)
    (?throws (e/select s) ClosedSelectorException)
    (?throws (e/select s :timeout 0) ClosedSelectorException)
    (?throws (e/select s :timeout 10) ClosedSelectorException)
    (?throws (e/conj! s t) ClosedSelectorException)))

(deftest closing-event
  (let [t (e/trigger 123)
        s (e/trigger-set t)]
    (?= (.provider t) s)
    (e/touch! t)
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

;remove touch!, use start!

;;
;; Selectors
;;

;simple selector
;empty selector set doesn't block
;cannot trigger selector twice
;can use several events
;selecting with timeout
;interrupting timer
;canceling event
;closing selector provider
;adding selector to wrong set

