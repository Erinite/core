(ns erinite.interceptors.default)

(defn attach-db
  [db]
  {:name :attach-db
   :enter #(assoc-in % [:request :db] db)})

(defn attach-services
  [services]
  {:name :attach-services
   :enter #(update-in % [:request :services] merge services)})

(defn attach-component-data
  [data]
  {:name :attach-component-data
   :enter #(update-in % [:request :component-data] merge data)})
