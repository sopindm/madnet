(ns madnet.event.timer-test
  (:require [khazad-dum :refer :all]
            [madnet.event :as e])
  (:import [java.nio.channels ClosedSelectorException]))

(deftest making-simple-timer
  (let [t (e/timer 0)
        s (e/timer-set)]
    (e/conj! s t)
    (?= (seq (e/signals s)) [t])
    (?= (.provider t) s)
    (e/emit! t)
    (?= (seq (e/select s)) [t])))

(deftest making-timer-with-timeout
  (let [t (e/timer 3)
        s (e/timer-set t)]
    (e/emit! t)
    (?= (seq (e/select s :timeout 0)) nil)
    (Thread/sleep 4)
    (?= (seq (e/select s :timeout 0)) [t])))

(deftest timer-blocking-select
  (let [t (e/timer 2)
        s (e/timer-set t)]
    (e/emit! t)
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
    (doall (map e/emit! [t1 t2]))
    (?= (set (e/select s)) #{t1 t2})))

(deftest starting-no-registered-timer
  (?throws (e/emit! (e/timer 1)) NullPointerException))

(deftest stopping-timer
  (let [t (e/timer 1)
        s (e/timer-set t)]
    (e/emit! t)
    (e/stop! t)
    (?= (seq (e/select s)) nil))
  (let [[t1 t2] (repeatedly 2 #(e/timer 1))
        s (e/timer-set t1 t2)]
    (doall (map e/emit! [t1 t2]))
    (e/stop! t1)
    (?= (seq (e/select s)) [t2])))

(deftest selecting-timer-with-timeout
  (let [t (e/timer 5)
        s (e/timer-set t)]
    (e/emit! t)
    (?= (seq (e/select s :timeout 3)) nil)
    (?= (seq (e/select s :timeout 3)) [t])))

(deftest canceling-timer
  (let [t (e/timer 0)
        s (e/timer-set t)]
    (e/cancel! t)
    (?= (seq (e/select s)) nil)
    (?= (.provider t) nil)
    (?= (seq (e/signals s)) nil)
    (?= (seq (.selections s)) nil))
  (let [t (e/timer 4)
        t2 (e/timer 5)
        s (e/timer-set t t2)]
    (e/emit! t)
    (e/emit! t2)
    (e/cancel! t)
    (?= (seq (e/select s)) [t2]))
  (let [t (e/timer 5)
        s (e/timer-set t)]
    (e/emit! t)
    (let [f (future (e/select s :timeout 5))]
      (Thread/sleep 2)
      (e/cancel! t)
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
    (e/emit! t)
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
    (e/emit! t)
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
    (?throws (e/conj! s (proxy [madnet.event.Signal] [])) IllegalArgumentException)
    (?throws (e/conj! (proxy [madnet.event.SignalSet] [] (conj [event] (.register event this))) t)
             IllegalArgumentException)))

