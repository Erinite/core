(ns erinite.interceptors.default
  (:require [duct.logger :as log]))

(defn attach-db
  [db]
  {:name :attach-db
   :enter (fn [context]
            (let [logger (get-in context [:request :logger])]
              (log/log logger :trace ::attach-db)
              (assoc-in context [:request :db] db)))})

(defn attach-services
  [services]
  {:name :attach-services
   :enter (fn [context]
            (let [logger (get-in context [:request :logger])]
              (log/log logger :trace ::attach-services {:services (vec (keys services))})
              (update-in context [:request :services] merge services)))})
   
