(ns erinite.services.queue
  (:require [integrant.core :as ig]))

(defprotocol Queue
  (enqueue! [conn message] "Enqueue a message"))

(defrecord Boundary [conn])

(defmethod ig/init-key :erinite.services/queue
  [_ conn]
  (->Boundary conn))
