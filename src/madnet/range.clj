(ns madnet.range
  (:refer-clojure :exclude [take drop sequence take-last drop-last range proxy])
  (:import [madnet.range Range ProxyRange ObjectRange]))

;;
;; Range operation wrappers
;;

(defn- proxy-method- [option]
  (case option
    :read-only '(writeImpl [_] nil)
    :write-only '(readImpl [_] nil)
    option))

(defmacro proxy [range & options-and-methods]
  (if (empty? options-and-methods)
    `(ProxyRange. ~range)
    `(clojure.core/proxy [ProxyRange] [~range]
       ~@(map proxy-method- options-and-methods))))

(defn size [^Range range]
  (.size range))

(defn take! [n ^Range range]
  (.take range n))

(defn take-last! [n ^Range range]
  (.takeLast range n))

(defn drop! [n ^Range range]
  (.drop range n))

(defn drop-last! [n ^Range range]
  (.dropLast range n))

(defn expand! [n ^Range range]
  (.expand range n))

(declare take)
(defn split! [n ^Range range]
  (let [split (take n range)]
    (drop! n range)
    split))

;;
;; Immutable range operations
;;

(defn take [n ^Range range]
  (take! n (.clone range)))

(defn take-last [n ^Range range]
  (take-last! n (.clone range)))

(defn drop [n ^Range range]
  (drop! n (.clone range)))

(defn drop-last [n ^Range range]
  (drop-last! n (.clone range)))

(defn expand [n ^Range range]
  (expand! n (.clone range)))

(defn split [n ^Range range]
  (let [clone (.clone range)]
    [(split! n clone) clone]))

