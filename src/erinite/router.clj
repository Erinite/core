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

(defn common-interceptors [base-interceptors logger]
  (into
   base-interceptors
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
    (coercion/coerce-request-interceptor)]))

(defn- invalid-interceptor
  [logger route-name interceptor-id & args] 
  (log/log logger :warn ::invalid-interceptor {:interceptor interceptor-id
                                               :route route-name}))

(defn transform-interceptor-vector
  [interceptors dynamic-interceptors registry logger route-name]
  (mapv
    (fn [interceptor]
      (if-let [[id & args] (and (vector? interceptor)
                                interceptor)]
        (apply (get registry id (partial invalid-interceptor logger route-name id)) args)
        (if (keyword? interceptor)
          ((get registry interceptor (partial invalid-interceptor logger route-name interceptor)))
          interceptor))) 
    (into interceptors
          dynamic-interceptors)))

(defn transform-interceptor-vectors
  [routes registry dynamic-interceptors logger]
  (walk/postwalk
    (fn [node]
      (if (and (map? node) (:interceptors node))
        (update node :interceptors transform-interceptor-vector (dynamic-interceptors node) registry logger (:name node))
        node))
    routes))


(defn create-router
  "Create a router from a collection of routes.
   Valid options:
    * `routes` - a vector of reitit routes
    * `interceptor-registry` - a erinite.interceptors/registry registry mapping keywords to interceptors
    * `dynamic-interceptors` - a function which generates a vector of interceptor keywords, given a reitit route, used to dynamically add interceptors based on route attributes
    * `interceptors` - an optional vector of interceptors to include in all routes
    * `corts-origins` - a vector of strings of allowed origins for CORS
    * `logger` - the duct logger used to report errors and to attach to requests"
  [{:keys [logger cors-origins routes interceptors dynamic-interceptors interceptor-registry]}]
  (wrap-cors
   (http/ring-handler
    (ring/router
     (transform-interceptor-vectors routes interceptor-registry (or dynamic-interceptors (constantly [])) logger)
     {:data {:coercion reitit.coercion.spec/coercion
             :interceptors (common-interceptors (or interceptors []) logger)}
      :coerce http/coerce-handler
      :compile http/compile-result
      ::http/default-options-handler ring/default-options-handler})
    (constantly {:status 404 :body "Not Found"})
    {:executor sieppari/executor})
   :access-control-allow-origin cors-origins
   :access-control-allow-methods [:get :put :post :delete]))

(defmethod ig/init-key :erinite/router
  [_ options]
  (create-router options))

