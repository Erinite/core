(ns erinite.services)

(defn extract
  [{:keys [services logger session]} & service-names]
  (let [service-list (map (partial get services) service-names)]
    (map
      #(assoc % :ctx {:logger logger
                      :session session})
      service-list)))
