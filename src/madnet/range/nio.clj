(ns madnet.range.nio
  (:refer-clojure :exclude [])
  (:require [madnet.range :as r])
  (:import [madnet.range]
           [madnet.range.nio Range]))

(defn range [begin end buffer]
  (Range. begin end buffer))

(defn byte-range [begin end buffer-size]
  (Range. begin end buffer-size))
