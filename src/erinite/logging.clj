(ns erinite.logging
  (:require [integrant.core :as ig]
            [duct.logger :as logger]))

(defmacro log
  [ctx level event data]
  `(let [session# (:session ~ctx)
         logger# (:logger ~ctx)
         data# (if session#
                 (assoc ~data :session/user (:user-id session#)
                             :session/account (:account-id session#)
                             :session/id (:id session#))
                 ~data)]
     (logger/-log logger# ~level
       ~(str *ns*) ~*file* ~(:line (meta &form))
       (delay (java.util.UUID/randomUUID))
       ~event data#)))

  
