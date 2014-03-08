(ns madnet.event
  (:refer-clojure :exclude [conj! loop])
  (:require [clojure.set :as s])
  (:import [madnet.event TriggerSignal TriggerSet TimerSignal TimerSet SelectorSignal SelectorSet
                         IEventHandler AEvent]
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
     (clojure.core/loop [coll# coll# iterator# iterator#]
       (if-not (.hasNext iterator#)
         (persistent! coll#)
         (let [value# (.next iterator#)
               conj# (clojure.core/conj! coll# value#)]
           (.remove iterator#)
           (recur conj# iterator#))))))

(defn attach! [signal attachment]
  (.attach signal attachment))

(defn attachment [signal]
  (.attachment signal))

(defn start! [& events]
  (doseq [e events] (.start e)))

(defn stop! [& events]
  (doseq [e events] (.stop e)))

(defn handle! [& events]
  (doseq [e events] (.handle e)))

(defn event-set [& events]
  (reduce conj! (madnet.event.MultiSignalSet.) events))

(defn- push-handler- [signal handler]
  (.pushHandler signal handler)
  signal)

(defn- push-signal- [handler signal]
  (.pushHandler signal handler)
  handler)

(defn handler
  ([f] (reify IEventHandler (onCallback [this a] (f a))))
  ([f & signals] (reduce push-signal- (handler f) signals)))

(defn event
  ([f] (proxy [AEvent IEventHandler] []
         (onCallback [arg]
           (let [arg (f arg)]
             (doseq [h (.handlers this)]
               (.onCallback h arg))))))
  ([f & signals] (reduce push-signal- (event f) signals)))

(defmacro defsignal [name [& args] & body]
  `(defn ~name
     ([~@args attachment#] (doto (~name ~@args) (attach! attachment#)))
     ([~@args] ~@body)))

;;
;; Flash signal
;;

(defsignal flash []
  (madnet.event.FlashSignal.))

;;
;; Trigger events and sets
;;

(defn trigger-set [& triggers]
  (reduce conj! (TriggerSet.) triggers))

(defn trigger
  ([] (TriggerSignal.))
  ([attachment] (doto (TriggerSignal.) (attach! attachment))))

;;
;; Timer events
;;

(defsignal timer [milliseconds]
  (TimerSignal. milliseconds))

(defn timer-set [& timers] (reduce conj! (TimerSet.) timers))

;;
;; Selectors
;;

(defsignal selector [channel operation]
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

(defn loop [event-set]
  (do-selections [e event-set] (.handle e))
  (recur event-set))
