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
          [:error e])))))

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
            (.printStackTrace result))

          :success
          (if on-success!
            (on-success! result)
            result))))))

(defmacro with-tx [[tx db] & body]
  `(in-async-tx
    (or (:spec ~db) ~db)
    (fn [~tx]
      ~@body)))

(defn call-with-tx
  [db f! & args]
  (in-async-tx
    db
    #(apply f! % args)))

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
     
