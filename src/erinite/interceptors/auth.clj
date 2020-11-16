(ns erinite.interceptors.auth
  (:require [integrant.core :as ig]
            [sieppari.context :as sc]
            [erinite.logging :refer [log log-exception] :as logging]))

(defn -get-token
  "Get token as either a bearer token in the authorization header, or as a sessionid in a cookie:
   Authorization: Bearer <token>
   Cookie: sessionid=<token>"
  [headers]
  (letfn [(extract-token
            [header regex]
            (some->> (get headers header)
                     (re-find regex)
                     (second)))]
    (or (extract-token "authorization" #"Bearer (.+)")
        (extract-token "cookie" #"sessionid=(.+?)(\;|$)"))))

(defn attach-session
  [session-valid?]
  {:name :attach-session
   :enter (fn [context]
            (let [request (:request context)
                  headers (:headers request)
                  token (-get-token headers)]
              (log request :info ::attach-session {:token token
                                                   :uri (:uri request)
                                                   :method (:request-method request)})
              (if-let [session (and token (try
                                            (session-valid? token)
                                            (catch Exception e
                                              (log-exception request :session/validation-error e)
                                              nil)))]
                (let [{:keys [account-id user-id session-id]} session]
                  (logging/set-context! session-id user-id account-id)
                  (log request :debug ::authenticated {:session/id session-id})
                  (assoc-in context [:request :session] session))
                (do
                  (log request :debug ::not-authorized)
                  (sc/terminate context {:status 401
                                         :body "Not Authorized"})))))})


(defmethod ig/init-key :erinite.interceptors/auth
  [_ {:keys [session-validation-fn]}]
  {:auth (partial attach-session session-validation-fn)})
