(ns madnet.channel
  (:refer-clojure :exclude [read])
  (:import [madnet.channel Channel]))

;;
;; Reading/writing
;;

(defn write! [dest src]
  (or (.write dest src) (.read src dest) (throw (UnsupportedOperationException.))))

(defn read! [dest src]
  (or (.read dest src) (.write src dest) (throw (UnsupportedOperationException.))))

(defn write [dest src]
  (let [writen (.clone dest)
        read (.clone src)]
    (write! writen read)
    [writen read]))

(defn read [dest src]
  (let [read (.clone dest)
        writen (.clone src)]
    (read! read writen)
    [read writen]))

  
