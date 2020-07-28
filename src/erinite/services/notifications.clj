(ns erinite.services.notifications
  (:require [integrant.core :as ig]))

(defprotocol Notifications
  (notify! [conn topic message] "Notify on a topic"))

(defrecord Boundary [conn])

(defmethod ig/init-key :erinite.services/notifications
  [_ conn]
  (->Boundary conn))
