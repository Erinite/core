(ns erinite.router
  (:require [integrant.core :as ig]
            [reitit.ring :as ring]
            [reitit.http :as http]
            [duct.logger :as log]
            [reitit.coercion.spec]
            [reitit.http.coercion :as coercion]
            [reitit.http.interceptors.parameters :as parameters]
            [muuntaja.interceptor :as muuntaja]
            [ring.middleware.cors :refer [wrap-cors]]
            [reitit.interceptor.sieppari :as sieppari]
            [clojure.walk :as walk]))

(defn attach-logger
  [logger]
  {:name :attach-logger
   :enter (fn [context]
            (assoc-in context [:request :logger] logger))})

(defn attach-data-interceptor
  []
  {:enter (fn [ctx]
            (let [request (:request ctx)
                  method (:request-method request)
                  data (-> request (ring/get-match) :data)
                  route-data (merge data (get data method))]
              (update ctx :request assoc :data route-data)))})

(defn common-interceptors [logger]
  [;; include logger in request
   (attach-logger logger)
   ;; include route-data in request
   (attach-data-interceptor)
   ;; query-params & form-params
   (parameters/parameters-interceptor)
   ;; content-negotiation
   (muuntaja/format-negotiate-interceptor)
   ;; encoding response body
   (muuntaja/format-response-interceptor)
   ;; decoding request body
   (muuntaja/format-request-interceptor)
   ;; coercing response bodys
   (coercion/coerce-response-interceptor)
   ;; coercing request parameters
   (coercion/coerce-request-interceptor)])

(defn- invalid-interceptor
  [logger name & args] 
  (log/log logger :warn ::invalid-interceptor-id {:name name}))

(defn transform-interceptor-vector
  [interceptors registry logger]
  (mapv
    (fn [interceptor]
      (if-let [[id & args] (and (vector? interceptor)
                                interceptor)]
        (apply (get registry id (partial invalid-interceptor logger)) args)
        (if (keyword? interceptor)
          ((get registry interceptor (partial invalid-interceptor logger interceptor)))
          interceptor))) 
    interceptors))

(defn transform-interceptor-vectors
  [routes registry logger]
  (walk/postwalk
    (fn [node]
      (if-let [interceptors (and (map? node)
                                 (:interceptors node))]
        (update node :interceptors transform-interceptor-vector registry logger) 
        node))
    routes))

(defmethod ig/init-key :erinite/router
  [_ {:keys [logger cors-origins routes interceptor-registry]}]
  (wrap-cors
    (http/ring-handler
      (ring/router
        (transform-interceptor-vectors routes interceptor-registry logger)
        {:data {:coercion reitit.coercion.spec/coercion
                :interceptors (common-interceptors logger)}
         :coerce http/coerce-handler
         :compile http/compile-result
         ::http/default-options-handler ring/default-options-handler})
      (constantly {:status 404 :body "Not Found"})
      {:executor sieppari/executor})
    :access-control-allow-origin cors-origins
    :access-control-allow-methods [:get :put :post :delete]))

