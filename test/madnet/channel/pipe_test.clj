(ns madnet.channel.pipe-test
  (:require [khazad-dum :refer :all]
            [madnet.channel :as c]
            [madnet.event :as e]
            [madnet.sequence :as s])
  (:import [madnet.channel Result]))

;;
;; General functions
;;

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
    (c/read p d)
    (.close p)
    (?throws (c/write p s) java.nio.channels.ClosedChannelException)
    (?throws (c/read p d) java.nio.channels.ClosedChannelException)))

(deftest pipe-reader-and-writer
  (let [p (c/pipe)
        r (.reader p)
        w (.writer p)]
    (?true (c/open? r))
    (?true (c/open? w))
    (.close r)
    (?false (c/open? r))
    (?true (c/open? w))
    (.close w)
    (?false (c/open? w))))

(deftest pipe-closeable-implementation
  (let [p (c/pipe)
        r (.reader p)
        w (.writer p)]
    (with-open [^java.io.Closeable reader r])
    (?false (c/open? r))
    (with-open [^java.io.Closeable writer w])
    (?false (c/open? w)))
  (let [p (c/pipe)]
    (with-open [pipe p])
    (?false (c/open? p))))

;writing to reader and reading from writer

;;
;; Events
;;

(deftest registering-channel-in-event-set
  (with-open [p (c/pipe)]
    (let [e (c/events p)
          s (e/event-set)]
      (c/register p s)
      (?= (set (e/signals s)) #{(.onRead e) (.onWrite e)}))))

(deftest registering-pipe-reader-and-writer
  (with-open [pipe (c/pipe)]
    (let [reader (.reader pipe)
          writer (.writer pipe)]
      (let [s (e/event-set)]
        (c/register reader s)
        (?= (seq (e/signals s)) [(-> pipe .events :on-read)]))
      (let [s (e/event-set)]
        (c/register writer s)
        (?= (seq (e/signals s)) [(-> pipe .events :on-write)])))))

(deftest pipe-events-attachment
  (with-open [pipe (c/pipe)]
    (let [e (c/events pipe)]
      (?= (e/attachment (:on-read e)) (.reader pipe))
      (?= (e/attachment (:on-write e)) (.writer pipe))
      (?= (e/attachment (:on-close e)) pipe))))

(deftest pipe-on-read-and-on-write-events
  (with-open [pipe (c/pipe)]
    (let [e (c/events pipe)
          s (e/event-set)]
      (c/register pipe s)
      (?= (e/for-selections [e s :timeout 0] e) [(.onWrite e)])
      (c/write pipe (s/wrap (byte-array 10)))
      (?= (e/for-selections [e s :timeout 0 :into #{}] e)
          #{(.onRead e) (.onWrite e)}))))

(deftest pipe-closing-events
  (with-open [pipe (c/pipe)]
    (let [a (atom [])
          s (e/event-set)
          on-read-closed (-> pipe .reader .events .onClose)
          on-write-closed (-> pipe .writer .events .onClose)
          on-closed (-> pipe .events :on-close)
          e (e/event ([e s] (swap! a conj {:e e :s s}))
                     on-read-closed on-write-closed on-closed)]
      (c/register pipe s)
      (.close (.reader pipe))
      (?= @a [{:e on-read-closed :s (.reader pipe)}])
      (?= (.attachment on-read-closed) nil)
      (.close (.writer pipe))
      (?= (set @a)  #{{:e on-read-closed :s (.reader pipe)}
                      {:e on-write-closed :s (.writer pipe)}
                      {:e on-closed :s pipe}})
      (?= (.attachment on-write-closed) nil)
      (?= (.attachment on-closed) nil)
      (?= (.provider (-> pipe .events :on-read)) nil)
      (?= (.provider (-> pipe .events :on-write)) nil)
      (.close s)))
  (let [a (atom [])
        pipe (c/pipe)
        on-read-closed (-> pipe .reader .events .onClose)
        on-write-closed (-> pipe .writer .events .onClose)
        on-closed (-> pipe .events :on-close)
        s (e/event-set)
        e (e/event ([_ s] (swap! a conj s))
                   on-read-closed on-write-closed on-closed)]
    (c/register pipe s)
    (.close pipe)
    (?= (set @a) #{pipe (.reader pipe) (.writer pipe)})
    (.close s)))

(deftest pipe-with-closed-read-and-write-is-closed
  (let [p (c/pipe)
        a (atom [])
        h (e/handler ([_ s] (swap! a conj s)) (-> p .events :on-close))]
    (-> p .reader .close)
    (-> p .writer .close)
    (?false (c/open? p))
    (?= @a [p])))

(deftest reading-from-closed-pipe
  (let [p (c/pipe)
        a (atom [])
        h (e/handler ([_ s] (swap! a conj s))
                     (-> p .reader .events .onClose))
        s (e/event-set)]
    (c/register p s)
    (.close (.writer p))
    (?= (seq (e/select s :timeout 0))
        [(-> p .reader .events .onRead)])
    (c/write (s/sequence [10 :element :byte]) p)
    (?= (seq @a) [(.reader p)])))

(deftest writing-to-closed-pipe
  (let [p (c/pipe)
        a (atom [])
        h (e/handler ([_ s] (swap! a conj s))
                     (-> p .writer .events .onClose))
        s (e/event-set)]
    (c/register p s)
    (.close (.reader p))
    (?= (seq (e/select s :timeout 0)) [(-> p .writer .events .onWrite)])
    (c/write p (s/wrap (byte-array 10)))
    (?= (seq @a) [(.writer p)])))

;pushing to pipe
;pushing to full pipe
;popping from pipe
;popping from empty pipe

