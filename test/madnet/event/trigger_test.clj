(ns madnet.test.trigger-test
  (:require [khazad-dum :refer :all]
            [madnet.event :as e]))

(deftest simple-triggering
  (let [s (e/trigger-set)
        e (e/trigger)]
    (e/conj! s e)
    (e/emit! e)
    (?= (seq (e/select s)) [e])))

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
             IllegalArgumentException))))


