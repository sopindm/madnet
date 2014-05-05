(ns madnet.channel
  (:refer-clojure :exclude [pop!]))

;;
;; State and events
;;

(defn open? [channel] (.isOpen channel))
(defn close [channel] (.close channel))
(defn on-close [channel] (.onClose channel))

(defn active? [channel] (.isActive channel))
(defn on-active [channel] (.onActive channel))

(defn register [channel set] (.register channel set))

;;
;; Reading/writing
;;

(defn write! [c1 c2] (.write c1 c2))
(defn read! [c1 c2] (.read c1 c2)) 

(defn tryPush! [channel obj] (.tryPush channel obj))
(defn push! [channel obj] (.push channel obj))
(defn push-in! [channel obj milliseconds] (.pushIn channel obj milliseconds))

(defn tryPop! [channel] (.tryPop channel))
(defn pop! [channel] (.pop channel))
(defn pop-in! [channel milliseconds] (.popIn channel milliseconds))
