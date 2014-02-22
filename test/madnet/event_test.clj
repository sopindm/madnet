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
    (?= (seq (e/select! s)) [t])))

(deftest touching-trigger-twice-has-no-effect
  (let [t (e/trigger)
        s (e/trigger-set t)]
    (e/touch! t)
    (e/touch! t)
    (?= (seq (e/select! s)) [t])))

(deftest touching-several-triggers
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (dorun (map e/touch! [t1 t2]))
    (?= (set (e/select! s)) #{t1 t2})))

(deftest touching-set-with-several-triggers
  (let [[t1 t2] (repeatedly 2 e/trigger)
        s (e/trigger-set t1 t2)]
    (e/touch! t1)
    (?= (seq (e/select! s)) [t1])))

(deftest select!-is-destructive
  (let [t (e/trigger)
        t2 (e/trigger)
        s (e/trigger-set t t2)]
    (e/touch! t)
    (?= (seq (e/select! s)) [t])
    (e/touch! t2)
    (?= (seq (e/select! s)) [t2])))

(deftest select!-is-blocking
  (let [t (e/trigger)
        s (e/trigger-set t)
        f (future (e/select! s))]
    (Thread/sleep 10)
    (?false (realized? f))
    (e/touch! t)
    (let [a (agent f)]
      (send-off a #(deref %))
      (when (not (await-for 1000 a)) (throw (RuntimeException. "Agent timeout")))
      (?= (seq @a) [t]))))

;one trigger in multiple sets

;triggers attachment
;triggers touch value

;select! now
;select! in

;interrupting selection
;canceling triggers
;canceling provider

;one shot triggers

;adding trigger to wrong set
;addint wrong event to trigger set

;;
;; Timeouts
;;

