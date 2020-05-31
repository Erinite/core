(ns erinite.routes)

(defmacro with-names
  "Compile-time add names to endpoints map values.
   Eg: {:route-1 {:some :data}
        :route-2 {:other :things}}
   would become:
       {:route-1 {:name ::route-1
                  :some :data}
        :route-2 {:name ::route-2
                  :other :things}}"
  [endpoints]
  (let [namespace (str (ns-name  *ns*))]
    (->> endpoints
         (map (fn [[k v]] [k (assoc v :name (keyword namespace (name k)))]))
         (into {}))))
