(ns erinite.services.notifications
  (:require [integrant.core :as ig]))

(defprotocol Notifications
  (listen! [conn topic-handlers] "Lisen for notifications on a topic")
  (notify! [conn topic message] "Notify on a topic")
  (close! [conn listener] "Close a listener"))

(defrecord Boundary [conn]
  Notifications
  (listen! [_ topic-handlers] (listen! conn topic-handlers))
  (notify! [_ topic message] (notify! conn topic message))
  (close! [_ listener] (close! conn listener)))

(defmethod ig/init-key :erinite.services/notifications [_ notifications]
  (assert (satisfies? notifications Notifications))
  (->Boundary notifications))
