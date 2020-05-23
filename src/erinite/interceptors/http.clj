(ns erinite.interceptors.http)

(def status->codes
  {:ok {:code 200 :body (fnil identity "Ok")}
   :created {:code 201 :body (fnil identity "Created")}
   :accepted {:code 202 :body (fnil identity "Accepted")}
   :redirect {:code 307 :body (constantly "Redirect") :headers (fn [url] {"Location" url})}
   :user-error {:code 400 :body (constantly "Bad Request")}
   :invalid {:code 400 :body (constantly "Bad Request")}
   :not-authorized {:code 401 :body (constantly "Not Authorized")}
   :forbidden {:code 403 :body (constantly "Forbidden")}
   :not-found {:code 404 :body (constantly "Not Found")}
   :too-many-requests {:code 429 :body (constantly "Too Many Requests")}
   :server-error {:code 500 :body (constantly "Internal Server Error")}
   :bad-gateway {:code 502 :body (constantly "Bad Gateway")}
   :unavailable {:code 503 :body (constantly "Service Unavailable")}
   :gateway-timeout {:code 504 :body (constantly "Gateway Timeout")}})

(defn translate-responses
  "Translate responses in the form [status data] to ring responses:
   Translated handler return values:
     `[:ok response]` -- 200, with `response` as body
     `[:created responese]` -- 201, with `response` as body
     `[:accepted response]` -- 202, with `response` as body
     `[:redirect url]` -- 307, with `url` as `Location` header
     `[:not-found]` -- 404
     `[:not-authorized]` -- 401
     `[:forbidden]` -- 403
     `[:user-error]` -- 400
     `[:invalid]` -- 400
     `[:too-many-requests]` -- 429
     `[:server-error]` -- 500
     `[:unavailable]` -- 503
   "
  []
  {:name :translate-responses
   :leave (fn [{:keys [response] :as context}]
            (if-let [response (when-let [[status & [data]] (and (vector? response) response)]
                                (when-let [{:keys [code body headers]} (get status->codes status)]
                                  {:status code
                                   :headers ((or headers (constantly {})) data)
                                   :body (body data)}))]
              (assoc context :response response)
              context))})
