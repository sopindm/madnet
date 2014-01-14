(ns madnet.sequence.nio
  (:use [clojure.core :as cl])
  (:import [madnet.sequence IBuffer]
           [madnet.sequence.nio ByteBuffer]))

(defn byte-buffer [size]
  (ByteBuffer. size))
