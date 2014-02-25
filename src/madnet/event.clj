(ns madnet.event
  (:refer-clojure :exclude [conj!])
  (:require [clojure.set :as s])
  (:import [madnet.event TriggerEvent TriggerSet TimerEvent TimerSet]))

;;
;; Abstract events and sets
;;

(defn attach! [event attachment]
  (.attach event attachment))

(defn attachment [event]
  (.attachment event))

(defn conj! [set event]
  (.register event set)
  set)

(defn events [set]
  (.events set))

(defn select [set & {:as options}]
  (let [timeout (:timeout options)
        now? (and timeout (zero? timeout))]
    (cond now? (.selectNow set)
          timeout (.selectIn set timeout)
          :else (.select set))
    (.selections set)))

(defn interrupt [set]
  (.interrupt set))

(defn cancel [x]
  (.cancel x))

(defn close [x]
  (.close x))


(defmacro do-selections [[var selector] & body]
  `(let [selections# (select ~selector)
         iterator# (.iterator selections#)]
     (while (.hasNext iterator#)
       (let [~var (.next iterator#)]
         ~@body)
       (.remove iterator#))
     nil))

(defmacro for-selections [[var selector] & body]
  `(let [selections# (select ~selector)
         iterator# (.iterator selections#)
         coll# (transient [])]
     (while (.hasNext iterator#)
       (clojure.core/conj! coll# (.next iterator#))
       (.remove iterator#))
     (persistent! coll#)))

;;
;; Trigger events and sets
;;

(defn trigger-set [& triggers]
  (reduce conj! (TriggerSet.) triggers))

(defn trigger
  ([] (TriggerEvent.))
  ([attachment] (doto (TriggerEvent.) (attach! attachment))))

(defn touch! [& triggers]
  (doseq [trigger triggers] (.touch trigger)))

;;
;; Timer events
;;

(defn timer
  ([milliseconds] (TimerEvent. milliseconds))
  ([milliseconds attachment] (doto (timer milliseconds) (attach! attachment))))

(defn timer-set [& timers]
  (reduce conj! (TimerSet.) timers))

(defn start! [timer]
  (.start timer))

(defn stop! [timer]
  (.stop timer))
