(ns madnet.event
  (:refer-clojure :exclude [conj! disj!])
  (:require [clojure.set :as s])
  (:import [java.nio.channels SelectionKey]
           [madnet.event EventHandler Event]))

;;
;; ISet
;;

(defn- conj-into-
  ([value] value)
  ([value set] (.conj set value) value)
  ([value set & more-sets]
     (reduce conj-into- (conj-into- value set) more-sets)))

(defn conj!
  ([set] set)
  ([set value] (.conj set value) set)
  ([set value & more-values] 
     (reduce conj! (conj! set value) more-values)))

(defn disj!
  ([set] set)
  ([set value] (.disj set value) set)
  ([set value & more-values]
     (reduce disj! (disj! set value) more-values)))

;;
;; Generic events/handlers
;;

(defmacro handler- [class setup [[emitter source] & handler]
                    & events-and-options]
  `(~setup (proxy [~class] []
                     (call [~(or emitter '_) ~(or source '_)]
                       ~@handler))
                   ~@events-and-options))

(defmacro handler [[[emitter source] & handler] & events]
  `(handler- EventHandler #'conj-into-
             ([~emitter ~source] ~@handler) ~@events))

(defn- setup-event- 
  ([event] event)
  ([event & args]
     (let [events (remove keyword? args)
           options (filter keyword? args)]
       (when (some #{:one-shot} options) (.oneShot event true))
       (apply conj-into- event events))))

(defmacro event
  ([] `(Event.))
  ([[[emitter source] & handler] & events]
     `(handler- Event #'setup-event-
                ([~emitter ~source] ~@handler) ~@events)))

(defn attach! [event obj] (.attach event obj) event)
(defn attachment [event] (.attachment event))

(defn emitters [handler]
  (.emitters handler))

(defn handlers [event]
  (.handlers event))

(defn emit!
  ([event] (.emit event) event)
  ([event obj] (attach! event obj) (emit! event)))

(defn when-any
  ([] (proxy [Event] []
        (call [e s] (.handle this (or (.attachment this) s)))))
  ([& events] (apply conj-into- (when-any) events)))
         
(defn when-every
  ([] (doto (proxy [Event] []
              (call [e s]
                (disj! e this)
                (when (-> this .emitters .isEmpty)
                  (.handle this (or (.attachment this) s)))))
        (.oneShot true)))
  ([& events] (apply conj-into- (when-every) events)))

(comment 
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
  (recur event-set)))
