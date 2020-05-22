(ns erinite.router
  (:require [erinite.utils :as utils]
            [integrant.core :as ig]
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

(defn enrich-request-interceptor
  [logger]
  {:name :enrich-request
   :enter (fn [context]
            (let [request (:request context)
                  correlation-id (or (get-in request [:headers "x-correlation-id"])
                                     (utils/make-id))
                  method (:request-method request)
                  data (-> request (ring/get-match) :data)
                  route-data (merge data (get data method))]
              (->> (assoc request
                          :logger logger
                          :data route-data
                          :correlation-id correlation-id)
                   (assoc context :request))))})

(defn common-interceptors [base-interceptors logger]
  (into
   base-interceptors
   [;; Enrich the request by adding the logger, route data and correlation-id
    (enrich-request-interceptor logger)
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


(defn -make-interceptor
  [logger registry route-name interceptor-id args]
  (if-let [ctor (get registry interceptor-id)]
    (apply ctor args)
    (log/log logger :warn ::invalid-interceptor {:interceptor interceptor-id
                                                 :route route-name})))

(defn transform-interceptor-vector
  [interceptors dynamic-interceptors registry logger route-name]
  (mapv
    (fn [interceptor]
      (if-let [[id & args] (and (vector? interceptor)
                                interceptor)]
        (-make-interceptor logger registry route-name id args)
        (if (keyword? interceptor)
          (-make-interceptor logger registry route-name interceptor [])
          interceptor))) 
    (into (or interceptors [])
          dynamic-interceptors)))

(defn transform-interceptor-vectors
  [routes registry dynamic-interceptors logger]
  (walk/postwalk
    (fn [node]
      (let [dynamic-interceptors (dynamic-interceptors node)]
        (if (and (map? node) 
                 (or (:interceptors node) dynamic-interceptors))
          (update node :interceptors transform-interceptor-vector dynamic-interceptors registry logger (:name node))
          node)))
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

