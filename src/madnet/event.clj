(ns madnet.event
  (:refer-clojure :exclude [conj!])
  (:require [clojure.set :as s])
  (:import [madnet.event TriggerEvent TriggerSet]))

(defn conj! [set event]
  (.register event set)
  set)

(defn select! [set]
  (.select set)
  (.selections set))

(defn trigger-set [& triggers]
  (reduce conj! (TriggerSet.) triggers))

(defn trigger []
  (TriggerEvent.))

(defn touch! [trigger]
  (.touch trigger))
