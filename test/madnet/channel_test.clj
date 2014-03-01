(ns madnet.channel-test
  (:require [madnet.range-test :refer [?range=]]
            [madnet.channel :as c]
            [madnet.range :as r]
            [madnet.sequence :as s]
            [madnet.event :as e]
            [khazad-dum :refer :all])
  (:import [madnet.channel Result]))

(deftest closing-channels
  (letfn [(channel- []
            (let [readable (atom true)
                  writeable (atom true)]
              (reify madnet.channel.IChannel
                (readable [this] @readable)
                (closeRead [this] (reset! readable false))
                (writeable [this] @writeable)
                (closeWrite [this] (reset! writeable false)))))]
    (let [c (channel-)]
      (c/close! c :read)
      (?false (c/readable? c)) 
      (?true (c/writeable? c)))
    (let [c (channel-)]
      (c/close! c :write)
      (?true (c/readable? c)) 
      (?false (c/writeable? c)))
    (let [c (channel-)]
      (c/close! c)
      (?false (c/readable? c)) 
      (?false (c/writeable? c)))))

