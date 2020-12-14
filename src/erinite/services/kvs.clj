(ns erinite.services.kvs
  (:refer-clojure :exclude [get])
  (:require [integrant.core :as ig]))

(defprotocol KeyValueStore
  (get [conn key])
  (get-all [kvs keys])
  (put! [conn key value])
  (put-all! [kvs key-value-map]))

(deftype Boundary [instance])

(defmethod ig/init-key :erinite.services/kvs
  [_ instance]
  (->Boundary instance))
