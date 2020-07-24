(ns erinite.services.queue
  (:require [integrant.core :as ig]))

(defprotocol Queue
  (open! [conn] "Open connection to the queue")
  (enqueue! [conn message] "Enqueue a message")
  (listen! [conn callback] "Listen for an dequeue a message")
  (close! [conn] "Close connection to the queue"))

(defrecord Boundary [conn]
  Queue
  (open! [_] (open! conn))
  (enqueue! [_ message] (enqueue! conn message))
  (listen! [_ callback] (listen! conn callback))
  (close! [_] (close! conn)))

(defmethod ig/init-key :erinite.services/queue [_ queue]
  (assert (satisfies? queue Queue))
  (->Boundary queue))
