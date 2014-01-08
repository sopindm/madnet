(ns madnet.buffer)

(defprotocol IBuffer
  (size [this])
  (emptySequence [this]))