(ns erinite.router
  (:require [erinite.ids :as ids]
            [integrant.core :as ig]
            [reitit.ring :as ring]
            [reitit.http :as http]
            [reitit.coercion.spec :as spec-coercion]
            [reitit.http.coercion :as coercion]
            [reitit.http.interceptors.parameters :as parameters]
            [reitit.http.interceptors.exception :as exception]
            [reitit.http.interceptors.multipart :as multipart]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [erinite.interceptors.http :as erinite-http]
            [muuntaja.interceptor :as muuntaja]
            [muuntaja.core :as m]
            [ring.middleware.cors :refer [wrap-cors]]
            [reitit.interceptor.sieppari :as sieppari]
            [clojure.walk :as walk]
            [erinite.logging :as logging]))

(defn wrap-log-context [f]
  (fn [request]
    (binding [logging/*log-context* {}]
      (f request))))

(defn enrich-request-interceptor
  [logger]
  {:name :enrich-request
   :enter (fn [context]
            (let [request (:request context)
                  correlation-id (or (get-in request [:headers "x-correlation-id"])
                                     (ids/make-id))
                  method (:request-method request)
                  data (-> request (ring/get-match) :data)
                  route-data (merge data (get data method))]
              (set! logging/*log-context* {:correlation-id correlation-id})
              (->> (assoc request
                          :logger logger
                          :data route-data
                          :correlation-id correlation-id)
                   (assoc context :request))))})

(defn -default-error-handler-handler [callback exception request]
  {:status 500
   :body (callback exception request)})

(defn common-interceptors [base-interceptors logger {:keys [response-fn on-error-fn!] :as intercept-exceptions?}]
  (filterv
   seq
   (into
    [;; swagger feature
     swagger/swagger-feature
     ;; query-params & form-params
     (parameters/parameters-interceptor)
     ;; content-negotiation
     (muuntaja/format-negotiate-interceptor)
     ;; encoding response body
     (muuntaja/format-response-interceptor)
     ;; exception handling
     (when intercept-exceptions?
       (exception/exception-interceptor
        (merge
         exception/default-handlers
         {::exception/default (partial -default-error-handler-handler (or response-fn (constantly "Internal Server Error")))
          ::exception/wrap (fn [handler e request]
                             (let [error-info (merge
                                               (select-keys request [:uri :request-method]) ; By default, parameters and headers are not logged in case they contain sensitive data
                                               (when on-error-fn!
                                                 (on-error-fn! e request)))]
                               (logging/log-exception (if (:logger request) request {:logger logger}) ::exception e error-info))
                             (handler e request))})))
     ;; exceptions from coercion
     (coercion/coerce-exceptions-interceptor)
     ;; decoding request body
     (muuntaja/format-request-interceptor)
     ;; coercing response body
     (coercion/coerce-response-interceptor)
     ;; coercing request parameters
     (coercion/coerce-request-interceptor)
     ;; multipart
     (multipart/multipart-interceptor)
     ;; Enrich the request with route data, the logger and correlation-id
     (enrich-request-interceptor logger)]
   ; ; Insert any custom interceptors
    base-interceptors)))


(defn -make-interceptor
  [logger registry route-name interceptor-id args]
  (if-let [ctor (get registry interceptor-id)]
    (apply ctor args)
    (logging/log logger :warn ::invalid-interceptor {:interceptor interceptor-id
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

(defn -convert-status-kw->code
  [responses]
  (->> responses
       (map (fn [[k v]]
              (let [code (get-in erinite-http/status->codes [k :code] k)]
                [code v])))
       (into {})))

(defn transform-routes
  "Transform routes, to:
    1. convert interceptor vectors from vectors of keyword-named interceptors to sieppari interceptors registered with the interceptor-registry
    2. if translate-responses? is set, convert keyword responses to response codes in the :responses declaration"
  [routes registry dynamic-interceptors logger translate-responses?]
  (walk/postwalk
    (fn [node]
      (let [dynamic-interceptors (dynamic-interceptors node)]
        (if (map? node) 
          (cond-> node
            (or (:interceptors node)
                (and (or (:name node)
                         (:handler node))
                     dynamic-interceptors)) (update :interceptors transform-interceptor-vector dynamic-interceptors registry logger (:name node))
            (and (:responses node) translate-responses?) (update :responses -convert-status-kw->code))
          node)))
    routes))


(defn create-router
  "Create a router from a collection of routes.
   Valid options:
    * `routes` - a vector of reitit routes
    * `interceptor-registry` - a erinite.interceptors/registry registry mapping keywords to interceptors
    * `dynamic-interceptors` - a function which generates a vector of interceptor keywords, given a reitit route, used to dynamically add interceptors based on route attributes
    * `interceptors` - an optional vector of interceptors to include in all routes
    * `translate-responses?` - set to true to translate [:ret-code-kw body] to {:status <code> :body <data>}, to allow the use of descriptive keywords instead of numeric status codes (defaults to true)
    * `intercept-exceptions` - set to true to include exception interceptor (defaults to false), or a map with one or both keys {:response-fn rf :on-error-fn! ef}.
         -- `(fn rf [exception request])` should return the body of the HTTP response on error (string or map)
         -- `(fn re [exception request])` is a side-effecting function which can be used to record the error in an error tracking or monitoring system, its return value should be nil or a map, which is merged into the log statement
         In any case, if `intercept-exceptions` is truthy, the exception is logged as an error by the logger, return value from `re` is merged into the data map.
    * `cors-origins` - a vector of strings of allowed origins for CORS
    * `custom-ring-middleware` - ring middleware function, if provided, will wrap after CORS but before all interceptors
    * `logger` - the duct logger used to report errors and to attach to requests"
  [{:keys [logger cors-origins routes custom-ring-middleware interceptors dynamic-interceptors interceptor-registry translate-responses? intercept-exceptions] :or {translate-responses? true}}]
  (let [interceptors (cond->> (or interceptors [])
                       translate-responses? (into [(erinite-http/translate-responses)]))
        dynamic-interceptors (or dynamic-interceptors (constantly []))]
    (wrap-cors
     (wrap-log-context
      ((or custom-ring-middleware identity)
       (http/ring-handler
        (http/router
         (transform-routes routes interceptor-registry dynamic-interceptors logger translate-responses?)
         {:data {:coercion spec-coercion/coercion
                 :muuntaja m/instance
                 :interceptors (common-interceptors interceptors logger intercept-exceptions)}
          :coerce http/coerce-handler
          :compile http/compile-result})
        (constantly {:status 404 :body "Not Found"})
        {:executor sieppari/executor})))
     :access-control-allow-origin (map re-pattern cors-origins)
     :access-control-allow-methods [:get :put :post :delete])))

(defmethod ig/init-key :erinite/router
  [_ options]
  (create-router options))

