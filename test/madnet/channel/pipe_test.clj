(ns madnet.channel.pipe-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c]
            [madnet.event :as e]
            [madnet.sequence :as s])
  (:import [madnet.channel Result]))

(deftest making-pipe
  (let [p (c/pipe)]
    (?= (c/write! p (s/wrap (byte-array (map byte (range 10)))))
        (Result. 10 10))
    (let [s (s/sequence [10 :element :byte])]
      (?= (c/read! p s) (Result. 10 10))
      (?= (seq s) (range 10)))))

(deftest reading-from-empty-pipe
  (let [p (c/pipe)
        reader (agent p)
        s (s/sequence [10 :element :byte])]
    (send-off reader #(c/read % s))
    (if-not (await-for 1000 reader)
      (throw (RuntimeException. "Agent timeout")))))

(deftest overwriting-pipe
  (let [p (c/pipe)
        s (s/wrap (byte-array 1000000))
        writer (agent s)]
    (send-off writer #(c/write p %))
    (if (await-for 1000 writer) (?true (> (count s) 0))
        (throw (RuntimeException. "Agent timeout")))))

(deftest closing-pipe
  (let [p (c/pipe)
        s (s/wrap (byte-array (map byte (range 10))))
        d (s/sequence [10 :element :byte])]
    (c/write p s)
    (c/close! p :write)
    (?throws (c/write p s) java.nio.channels.ClosedChannelException)
    (c/read p d)
    (c/close! p :read)
    (?throws (c/read p d) java.nio.channels.ClosedChannelException)))

(deftest pipe-reader-and-writer
  (let [p (c/pipe)
        r (.reader p)
        w (.writer p)]
    (?true (c/readable? r))
    (?false (c/readable? w))
    (?false (c/writeable? r))
    (?true (c/writeable? w))))

(deftest pipe-closeable-implementation
  (let [p (c/pipe)
        r (.reader p)
        w (.writer p)]
    (with-open [^java.io.Closeable reader r])
    (?false (c/readable? r))
    (with-open [^java.io.Closeable writer w])
    (?false (c/writeable? w)))
  (let [p (c/pipe)]
    (with-open [pipe p])
    (?false (c/readable? p))
    (?false (c/writeable? p))))

(deftest registering-channel-in-event-set
  (with-open [p (c/pipe)]
    (let [e (c/events p)
          s (e/event-set)]
      (c/register p s)
      (?= (set (e/events s))
          #{(.onRead e) (.onWrite e)
            (.onReadClosed e) (.onWriteClosed e) (.onClosed e)}))))

(deftest registering-pipe-reader-and-writer
  (with-open [pipe (c/pipe)]
    (let [reader (.reader pipe)
          writer (.writer pipe)]
      (let [s (e/event-set)]
        (c/register reader s)
        (?= (set (e/events s))
            #{(-> pipe .events .onRead)
              (-> pipe .events .onReadClosed)}))
      (let [s (e/event-set)]
        (c/register writer s)
        (?= (set (e/events s))
            #{(-> pipe .events .onWrite)
              (-> pipe .events .onWriteClosed)})))))

(deftest pipe-on-read-and-on-write-events
  (with-open [pipe (c/pipe)]
    (let [e (c/events pipe)
          s (e/event-set)]
      (c/register pipe s)
      (e/start! (.onRead e) (.onWrite e))
      (?= (e/for-selections [e s :timeout 0] e) [(.onWrite e)])
      (c/write pipe (s/wrap (byte-array 10)))
      (?= (e/for-selections [e s :timeout 0 :into #{}] e)
          #{(.onRead e) (.onWrite e)}))))

(deftest pipe-closing-events
  (with-open [pipe (c/pipe)]
    (let [e (c/events pipe)
          s (e/event-set)]
      (c/register pipe s)
      (.closeRead pipe)
      (?= (e/for-selections [e s :timeout 0] e) [(.onReadClosed e)])
      (?= (.provider (.onReadClosed e)) nil)
      (.closeWrite pipe)
      (?= (e/for-selections [e s :timeout 0] e) [(.onWriteClosed e)])
      (?= (.provider (.onWriteClosed e)) nil)
      (.close pipe)))
  (let [pipe (c/pipe)
        e (c/events pipe)
        s (e/event-set)]
    (c/register pipe s)
    (.close pipe)
    (?= (e/for-selections [e s :timeout 0 :into #{}] e)
        #{(.onReadClosed e) (.onWriteClosed e) (.onClosed e)})
    (?= (.provider (.onReadClosed e)) nil)
    (?= (.provider (.onWriteClosed e)) nil)
    (?= (.provider (.onClosed e)) nil)))
