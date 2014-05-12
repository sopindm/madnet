(ns madnet.sequence
  (:refer-clojure :exclude [sequence get])
  (:require [madnet.channel :as c]))

;;
;; Sequence range
;;

(defn begin [seq] (.begin seq))
(defn end [seq] (.end seq))

(defn size [seq] (.size seq)) 
(defn free-space [seq] (.freeSpace seq))

;;
;; Sequence modifiers
;;

(defn take! [n seq] (.take seq n) seq)
(defn drop! [n seq] (.drop seq n) seq)
(defn expand! [n seq] (.expand seq n) seq)

;;
;; Reading/writing
;;

(defn get [seq n] (.get seq n))
