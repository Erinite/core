(ns erinite.services.queue
  (:require [integrant.core :as ig]))

(defprotocol Queue
  (enqueue! [queue message] "Enqueue a message"))

(deftype Boundary [queue])

(defmethod ig/init-key :erinite.services/queue
  [_ queue]
  (->Boundary queue))
