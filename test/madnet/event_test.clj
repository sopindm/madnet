(ns madnet.event-test
  (:require [khazad-dum :refer :all]
            [madnet.event :as e]))

;;
;; Triggers
;;

(deftest simple-triggering
  (let [s (e/trigger-set)
        t (e/trigger)]
    (e/conj! s t)
    (e/touch! t)
    (?= (seq (e/select s)) [t])))

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
  (let [s (e/trigger-set)
        f (future (e/select s :timeout 3))]
    (Thread/sleep 1)
    (?false (realized? f))
    (Thread/sleep 6)
    (?true (realized? f))))

(deftest interrupting-selection
  (let [s (e/trigger-set)
        f (future (e/select s))]
    (Thread/sleep 5)
    (?false (realized? f))
    (e/interrupt s)
    (Thread/sleep 2)
    (?true (realized? f))
    (?= (seq (.selections s)) nil)))
    
(deftest single-interrupt-does-not-interrupt-two-selections
  (let [s (e/trigger-set)
        f (future (e/select s)
                  (e/select s))]
    (Thread/sleep 2)
    (e/interrupt s)
    (Thread/sleep 2)
    (?false (realized? f))
    (e/interrupt s)
    (Thread/sleep 2)
    (?true (realized? f))))

;canceling triggers
;canceling provider

;one shot triggers

;adding trigger to wrong set
;adding wrong event to trigger set

;;
;; Timeouts
;;

