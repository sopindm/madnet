(ns madnet.sequence
  (:refer-clojure :exclude [sequence get set!])
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

(defn clone [seq] (.clone seq))

(defn take! [n seq] (.take seq n) seq)
(defn drop! [n seq] (.drop seq n) seq)
(defn expand! [n seq] (.expand seq n) seq)

;;
;; Reading/writing
;;

(defn get [seq n] (.get seq n))
(defn set! [seq n value] (.set seq n value))

;;
;; Sequence generator
;;

(defn- buffer- [size-or-coll type direct?]
  (let [size (if (integer? size-or-coll) size-or-coll (count size-or-coll))
        coll (if (integer? size-or-coll) nil size-or-coll)]
    (when (and (not= type :byte) (not (nil? direct?))) (throw (IllegalArgumentException.)))
    (case type 
      :object (object-array size-or-coll)
      :byte (if direct?
              (let [buffer (java.nio.ByteBuffer/allocateDirect size)]
                (when coll
                  (.put buffer (byte-array coll))
                  (.position buffer 0))
                buffer)
              (java.nio.ByteBuffer/wrap (byte-array size-or-coll)))
      :char (java.nio.CharBuffer/wrap (char-array size-or-coll)))))

(defn- reader- [type buffer]
  (case type
    :object (madnet.sequence.InputObjectSequence. buffer 0 (count buffer))
    :byte (madnet.sequence.InputByteSequence. (.duplicate buffer))
    :char (madnet.sequence.InputCharSequence. (.duplicate buffer))))

(defn- writer- [type buffer]
  (case type
    :object (madnet.sequence.OutputObjectSequence. buffer 0 (count buffer))
    :byte (madnet.sequence.OutputByteSequence. (.duplicate buffer))
    :char (madnet.sequence.OutputCharSequence. (.duplicate buffer))))

(defn- sequence- [size-or-coll element direct? read-only write-only]
  (let [buffer (buffer- size-or-coll element direct?)]
    (cond read-only (reader- element buffer)
          write-only (writer- element buffer)
          :else (let [reader (reader- element buffer)
                      writer (writer- element buffer)]
                  (proxy [madnet.sequence.IOSequence] [false]
                    (reader [] reader)
                    (writer [] writer))))))

(defn- setup-reader- [s size-or-coll read-at write-at]
  (let [size (if (integer? size-or-coll) size-or-coll (count size-or-coll))
        full? (not (integer? size-or-coll))]
    (let [[begin end] (cond read-at read-at
                            write-at [0 (first write-at)]
                            :else [0 (if full? size 0)])]
      (->> s (drop! begin) (take! end)))))

(defn- setup-writer- [s size-or-coll read-at write-at]
  (let [size (if (integer? size-or-coll) size-or-coll (count size-or-coll))
        full? (not (integer? size-or-coll))]
    (let [[begin end] (cond write-at write-at
                            read-at (let [re (+ (first read-at) (second read-at))] [re (- size re)])
                            :else [0 (if full? 0 size)])]
      (->> s (drop! begin) (take! end)))))

(defn sequence [size-or-coll & {:keys [reader writer element direct read-only write-only]
                                :or {element :object}}]
  (when (and read-only write-only) (throw (IllegalArgumentException.)))
  (let [s (sequence- size-or-coll element direct read-only write-only)
        full? (not (integer? size-or-coll))]
    (when (not write-only) (setup-reader- (if read-only s (c/reader s)) size-or-coll reader writer))
    (when (not read-only) (setup-writer- (if write-only s (c/writer s)) size-or-coll reader writer))
    s))



  
