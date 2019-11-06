(ns erinite.db
  (:require [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]
            [clojure.string :as string]
            [clojure.core.async :as async]
            [integrant.core :as ig]))

(defn -run-in-tx!
  [db f]
  (try
    (jdbc/with-db-transaction [tx db {:isolation :repeatable-read}]
      [:success (f tx)])
    (catch java.sql.SQLException e
      (let [sql-state (.getSQLState e)]
        (if (string/includes? sql-state "40001")
          [:retry nil]
          [:error e])))
    (catch Exception e
      [:error e])))

(defn in-async-tx
  "Run a function, f, inside a database transaction, retrying on concurrent"
  [db f & [{:keys [on-success! on-error! on-retry!]}]]
  (async/go
    (loop []
      (let [[status result] (-run-in-tx! db f)] 
        (case status
          :retry
          (do
            (when on-retry!  (on-retry!))
            (recur))

          :error
          (if on-error!
            (on-error! result)
            (do
              (.printStackTrace result)
              ::nil))

          :success
          (if on-success!
            (on-success! result)
            (if (nil? result) ::nil result)))))))

(defmacro with-tx [[tx db] & body]
  `(let [error-ch# (async/chan)
         result-ch# (in-async-tx
                      (or (:spec ~db) ~db)
                      (fn [~tx]
                        ~@body)
                      {:on-error! #(async/>!! error-ch# %)})]
    (let [[value# ch#] (async/alts!! [result-ch# error-ch#])]
      (if (= ch# result-ch#)
        (when-not (= value# ::nil) value#)
        (throw value#)))))

(defn call-with-tx
  [db f! & args]
  (let [p (promise)]
    (in-async-tx
      db
      #(apply f! % args)
      {:on-success! #(deliver p %)})
    p))

(defn stream-tx!
  [db f! {:keys [success error]}]
  (in-async-tx
    db
    f!
    {:on-success! (when success #(async/>!! success %))
     :on-error! (when error #(async/>!! error %))}))

(defmacro load-queries
  [component-id queries-file]
  `(do
    (def this-ns# *ns*)
    (hugsql/def-db-fns ~queries-file)
    (defmethod ig/init-key ~component-id [_# _#]
      (hugsql/def-db-fns ~queries-file {:ns this-ns#}))
    (defmethod ig/halt-key! ~component-id [_# _#])))
     
