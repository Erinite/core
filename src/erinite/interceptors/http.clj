(ns erinite.interceptors.http)

(def status->codes
  {:ok {:code 200 :body identity}
   :created {:code 201 :body identity}
   :accepted {:code 202 :body identity}
   :no-content {:code 204 :body (constantly nil)}
   :redirect {:code 307 :body (constantly nil) :headers (fn [url] {"Location" url})}
   :bad-request {:code 400}
   :invalid {:code 400}
   :not-authorized {:code 401}
   :forbidden {:code 403}
   :not-found {:code 404}
   :conflict {:code 409}
   :too-many-requests {:code 429}
   :server-error {:code 500}
   :bad-gateway {:code 502}
   :unavailable {:code 503}
   :gateway-timeout {:code 504}})

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
                                   :headers (when headers (headers data))
                                   :body (when body (body data))}))]
              (assoc context :response response)
              context))})
