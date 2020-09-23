(ns erinite.services.notifications
  (:require [integrant.core :as ig]))

(defprotocol Notifications
  (notify! [instance topic message] "Notify on a topic"))

(deftype Boundary [instance])

(defmethod ig/init-key :erinite.services/notifications
  [_ instance]
  (->Boundary instance))
