(ns madnet.event
  (:refer-clojure :exclude [conj! disj! loop])
  (:require [clojure.set :as s])
  (:import [java.nio.channels SelectionKey]
           [madnet.event Event EventHandler]))

;;
;; ISet
;;

(defn- conj-into-
  ([value] value)
  ([value set] (.$plus$eq set value) value)
  ([value set & more-sets]
     (reduce conj-into- (conj-into- value set) more-sets)))

(defn conj!
  ([set] set)
  ([set value] (.$plus$eq set value) set)
  ([set value & more-values] 
     (reduce conj! (conj! set value) more-values)))

(defn disj!
  ([set] set)
  ([set value] (.$minus$eq set value) set)
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
       (when (some #{:one-shot} options) (.oneShot_$eq event true))
       (apply conj-into- event events))))

(defmacro event
  ([] `(Event.))
  ([[[emitter source] & handler] & events]
     `(handler- Event #'setup-event-
                ([~emitter ~source] ~@handler) ~@events)))

(defn- scala-set [set] (scala.collection.JavaConversions/asJavaSet set))
(defn- scala-mset [set] (scala.collection.JavaConversions/mutableSetAsJavaSet set))

(defn emitters [handler] (scala-set (.emitters handler)))
(defn handlers [event] (scala-set (.handlers event)))

(defn emit! [event obj] (.emit event obj))

(defn when-any
  ([] (proxy [Event] [] (call [e s] (.handle this s))))
  ([& events] (apply conj-into- (when-any) events)))
         
(defn when-every
  ([] (doto (proxy [Event] []
              (call [e s]
                (disj! e this)
                (when (-> this .emitters .isEmpty)
                  (.handle this s))))
        (.oneShot_$eq true)))
  ([& events] (apply conj-into- (when-every) events)))

;;
;; Signals
;;

(defn attach! [signal obj] (.attach signal obj) signal)
(defn attachment [signal] (.attachment signal))

(defn start! [signal] (.start signal))
(defn stop! [signal] (.stop signal))

(defn signals [set] (scala-set (.signals set)))
(defn selections [set] (scala-mset (.selections set)))

(defn select [set & {:as options}]
  (let [timeout (:timeout options)
        now? (and timeout (zero? timeout))]
    (cond now? (.selectNow set)
          timeout (.selectIn set timeout)
          :else (.select set))
    (selections set)))

(defn cancel! [x] (if-let [provider (.provider x)] (disj! provider x)))

(defn persistent? [signal]
  (.persistent signal))

(defn set-persistent! [signal persistent?]
  (.persistent_$eq signal persistent?))

(defmacro do-selections [[var selector & options] & body]
  `(let [selections# (select ~selector ~@options)
         iterator# (.iterator selections#)]
     (while (.hasNext iterator#)
       (let [~var (.next iterator#)]
         ~@body)
       (.remove iterator#))))

(defmacro for-selections [[var selector & options] & body]
  `(let [selections# (select ~selector ~@options)
         iterator# (.iterator selections#)
         coll# (transient ~(or (:into (apply hash-map options)) []))]
     (clojure.core/loop [coll# coll# iterator# iterator#]
       (if-not (.hasNext iterator#)
         (persistent! coll#)
         (let [~var (.next iterator#)
               value# (do ~@body)
               conj# (clojure.core/conj! coll# value#)]
           (.remove iterator#)
           (recur conj# iterator#))))))

(defmacro defsignal [[name [& args] & body] [set-name & set-body]]
  `(do (defn ~name
         ([~@args attachment#] (doto (~name ~@args) (attach! attachment#)))
         ([~@args] ~@body))
       (defn ~set-name
         ([] ~@set-body)
         ([& triggers#] (reduce conj! (~set-name) triggers#)))))

(defsignal (trigger [] (madnet.event.TriggerSignal.))
  (trigger-set (madnet.event.TriggerSet.)))

(comment
(defsignal (timer [milliseconds] (madnet.event.TimerSignal. milliseconds))
  (timer-set (madnet.event.TimerSet.)))

(defsignal
  (selector [channel operation]
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
      (madnet.event.SelectorSignal. channel op-code)))
  (selector-set (madnet.event.SelectorSet.)))

(defn event-set
  ([] (madnet.event.MultiSignalSet.))
  ([& events] (reduce conj! (event-set) events)))

(defn loop [event-set]
  (do-selections [e event-set] (.handle e))
  (recur event-set)))
