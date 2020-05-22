(ns erinite.logging
  (:require [integrant.core :as ig]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre]
            [duct.core.merge :as merge]
            [duct.core :as core]
            [duct.logger :as logger]))

;; LARGELY COPIED FROM:
;; https://github.com/duct-framework/module.logging
;; https://github.com/duct-framework/logger.timbre
;; This copies the duct timbre logger, but serialises the data as JSON
;; before passsing it to timbre. Also acts like a duct module.

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; COPIED FROM:
;; https://github.com/duct-framework/logger.timbre
;; data has been wrapped in cheshire's generate-string

(defn brief-output-fn [{:keys [msg_]}]
  (force msg_))

(defn brief-appender [options]
  (-> (timbre/println-appender options)
      (assoc :output-fn brief-output-fn)
      (merge (select-keys options [:min-level]))))

(defmethod ig/init-key ::println [_ options]
  (-> (timbre/println-appender options)
      (merge (select-keys options [:min-level]))))

(defmethod ig/init-key ::spit [_ options]
  (-> (timbre/spit-appender options)
      (merge (select-keys options [:min-level]))))

(defmethod ig/init-key ::brief [_ options]
  (brief-appender options))

(defn- duct-log-format? [vargs]
  (and (<= 1 (count vargs) 2)
       (let [[evt data] vargs]
         (and (keyword?  evt)
              (namespace evt)
              (or (nil? data)
                  (map? data))))))

(defn wrap-legacy-logs [{:keys [vargs] :as data}]
  (cond-> data
    (not (duct-log-format? vargs))
    (assoc :vargs [::legacy vargs])))

(defrecord TimbreJsonLogger [config]
  logger/Logger
  (-log [_ level ns-str file line id event data]
    (let [data (json/generate-string data)]
      (cond
        (instance? Throwable data)
        (timbre/log! level :p (event)
                     {:config config, :?ns-str ns-str, :?file file, :?line line, :?err data
                      :?base-data {:id_ id}})
        (nil? data)
        (timbre/log! level :p (event)
                     {:config config, :?ns-str ns-str, :?file file, :?line line
                      :?base-data {:id_ id}})
        :else
        (timbre/log! level :p (event data)
                     {:config config, :?ns-str ns-str, :?file file, :?line line
                      :?base-data {:id_ id}})))))

(defn wrap-legacy-logs [{:keys [vargs] :as data}]
  (cond-> data
    (not (duct-log-format? vargs))
    (assoc :vargs [::legacy vargs])))

(defmethod ig/init-key :erinite.logging/timbre-json [_ config]
  (let [timbre-logger (->TimbreJsonLogger config)
        prev-root timbre/*config*]
    (if (:set-root-config? config)
      (let [config (update config :middleware (fnil conj []) wrap-legacy-logs)]
        (timbre/set-config! config)
        (assoc timbre-logger :prev-root-config prev-root))
      timbre-logger)))

(defmethod ig/halt-key! :erinite.logging/timbre-json [_ timbre]
  (when-let [prev-config (:prev-root-config timbre)]
    (timbre/set-config! prev-config)))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; COPIED FROM:
;; https://github.com/duct-framework/module.logging
;; duct.logger/timbre has been replaced with erinite.logging/timbre-json

(defn- get-environment [config options]
  (:environment options (::core/environment config :production)))

(def ^:private prod-config
  {:erinite.logging/timbre-json
   {:level     (merge/displace :info)
    :appenders ^:displace {::println (ig/ref ::println)}}
   ::println {}})

(def ^:private test-config
  {:erinite.logging/timbre-json
   {:level     (merge/displace :debug)
    :appenders ^:displace {::spit  (ig/ref ::spit)}}
   ::spit
   {:fname (merge/displace "logs/test.log")}})

(def ^:private dev-config
  {:erinite.logging/timbre-json
   {:level     (merge/displace :debug)
    :appenders ^:displace {::spit  (ig/ref ::spit)
                           ::brief (ig/ref ::brief)}}
   ::spit
   {:fname (merge/displace "logs/dev.log")}
   ::brief
   {:min-level (merge/displace :report)}})

(def ^:private env-configs
  {:production prod-config
   :development dev-config
   :test       test-config})

(defmethod ig/init-key :erinite/logging [_ options]
  #(core/merge-configs % (env-configs (get-environment % options))))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HELPERS
;; 

(defmacro log
  [ctx level event & [data]]
  `(let [logger# (:logger ~ctx)
         data#  (if-let [session# (:session ~ctx)]
                  (assoc ~data
                         :context {:user (:user-id session#)
                                   :account (:account-id session#)
                                   :session (:id session#)
                                   :correlationId (:correlation-id ~ctx)})
                  (assoc ~data :context {:correlationId (:correlation-id ~ctx)}))]
     (if logger#
       (logger/-log logger# ~level
                    ~(str *ns*) ~*file* ~(:line (meta &form))
                    (delay (java.util.UUID/randomUUID))
                    ~event data#)
       (println "No logger provided"
                ~(str "[" *ns* ":" *file* ":" (:line (meta &form)) "]:")
                ~level ~event ~data))
     nil))

(defn -add-exception-info
  [data e]
  (assoc data
         :message (.getMessage e)
         :exception (.. e getClass getSimpleName)
         :stack-trace (mapv str (.getStackTrace e))))

(defmacro log-exception
  [ctx event ex & [data]]
  `(log ~ctx :error ~event (-add-exception-info ~data ~ex)))
