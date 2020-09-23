(ns erinite.services.queue
  (:require [integrant.core :as ig]))

(defprotocol Queue
  (enqueue! [instance message] "Enqueue a message"))

(deftype Boundary [instance])

(defmethod ig/init-key :erinite.services/queue
  [_ instance]
  (->Boundary instance))
