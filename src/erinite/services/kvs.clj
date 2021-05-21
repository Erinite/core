(ns erinite.services.kvs
  (:refer-clojure :exclude [get range pop!])
  (:require [integrant.core :as ig]))

(defprotocol KeyValueStore
  (get [kvs key])
  (get-all [kvs keys])
  (put! [kvs key value])
  (put-all! [kvs key-value-map]))

(defprotocol KeyValueStack
  (push! [kvs key value])
  (pop! [vvs key])
  (top [kvs key])
  (range [kvs key from to])
  (length [kvs key]))

(deftype Boundary [instance])

(defmethod ig/init-key :erinite.services/kvs
  [_ instance]
  (->Boundary instance))
