(ns erinite.services.notifications
  (:require [integrant.core :as ig]))

(defprotocol Notifications
  (notify! [notification topic message] "Notify on a topic"))

(deftype Boundary [notification])

(defmethod ig/init-key :erinite.services/notifications
  [_ notification]
  (->Boundary notification))
