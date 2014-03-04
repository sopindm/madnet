(ns madnet.event
  (:refer-clojure :exclude [conj!])
  (:require [clojure.set :as s])
  (:import [madnet.event TriggerSignal TriggerSet TimerSignal TimerSet SelectorSignal SelectorSet]
           [java.nio.channels SelectionKey]))

;;
;; Abstract events and sets
;;

(defn conj! [set event]
  (.register event set)
  set)

(defn signals [set]
  (.signals set))

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


(defmacro do-selections [[var selector & options] & body]
  `(let [selections# (select ~selector ~@options)
         iterator# (.iterator selections#)]
     (while (.hasNext iterator#)
       (let [~var (.next iterator#)]
         ~@body)
       (.remove iterator#))
     nil))

(defmacro for-selections [[var selector & options] & body]
  `(let [selections# (select ~selector ~@options)
         iterator# (.iterator selections#)
         coll# (transient ~(or (:into (apply hash-map options)) []))]
     (while (.hasNext iterator#)
       (clojure.core/conj! coll# (.next iterator#))
       (.remove iterator#))
     (persistent! coll#)))

(defn start! [& events]
  (doall (map #(.start %) events)))

(defn stop! [& events]
  (doall (map #(.stop %) events)))

(defn event-set [& events]
  (reduce conj! (madnet.event.MultiSignalSet.) events))

;;
;; Trigger events and sets
;;

(defn trigger-set [& triggers]
  (reduce conj! (TriggerSet.) triggers))

(defn trigger []
  (TriggerSignal.))

;;
;; Timer events
;;

(defn timer
  ([milliseconds] (TimerSignal. milliseconds))
  ([milliseconds attachment] (doto (timer milliseconds) (attach! attachment))))

(defn timer-set [& timers]
  (reduce conj! (TimerSet.) timers))

;;
;; Selectors
;;

(defn selector [channel operation]
  (let [op-code (case operation
                  :read SelectionKey/OP_READ
                  :write SelectionKey/OP_WRITE
                  :accept SelectionKey/OP_ACCEPT
                  :connect SelectionKey/OP_CONNECT)]
    (letfn [(check-option [name type]
              (when (and (= operation name) (not (instance? type channel)))
                (throw (IllegalArgumentException.))))]
      (check-option :write java.nio.channels.WritableByteChannel)
      (check-option :read java.nio.channels.ReadableByteChannel)
      (check-option :accept java.nio.channels.ServerSocketChannel)
      (check-option :connect java.nio.channels.SocketChannel))
    (SelectorSignal. channel op-code)))

(defn selector-set [& events]
  (reduce conj! (SelectorSet.) events))

