(ns erinite.interceptors.registry
  (:require [integrant.core :as ig]
            [erinite.interceptors.default :as default]))

(defmethod ig/init-key :erinite.interceptors/registry
  [_ {:keys [interceptors]}]
  (->> interceptors
       (reduce merge)
       (into {:attach-db default/attach-db
              :attach-services default/attach-services})))
