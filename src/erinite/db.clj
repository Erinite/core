(ns erinite.db
  (:require [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.core.async :as async]
            [cheshire.generate :refer [add-encoder]]
            [integrant.core :as ig]))


(defn -run-in-tx!
  [db f]
  (try
    (jdbc/with-db-transaction [tx db {:isolation :repeatable-read}]
      [:success (f tx)])
    (catch java.sql.SQLException e
      (let [sql-state (.getSQLState e)]
        (if (and sql-state
                 (string/includes? sql-state "40001"))
          [:retry nil]
          [:error e])))
    (catch Exception e
      [:error e])))


(defn in-tx
  "Run a function, f, inside a database transaction, retrying on concurrent modification error"
  [db f & [{:keys [on-success! on-error! on-retry!]}]]
  (loop []
    (let [[status result] (-run-in-tx! db f)]
      (case status
        :retry
        (do
          (when on-retry! (on-retry!))
          (recur))

        :error
        (do
          (when on-error! (on-error! result))
          ::error)

        :success
        (if on-success!
          (on-success! result)
          result)))))


(defn in-async-tx
  ""
  [db f & [opts]]
  (async/go
   (if-let [result (in-tx db f opts)]
     result
     ::nil)))


(defmacro with-tx [[tx db] & body]
  `(let [error# (volatile! nil)
         result# (in-tx (or (:spec ~db) ~db)
                        (fn [~tx] ~@body)
                        {:on-error! #(vreset! error# %)})]
     (if (= result# ::error)
       (throw @error#)
       result#)))


(defn call-with-tx
  [db f! & args]
  (let [p (promise)]
    (in-async-tx
      db
      #(apply f! % args)
      {:on-success! #(deliver p %)
       :on-error! #(deliver p nil)})
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
     

(defn kw->pg-enum
  "Converts a keyword into a PGObject for a Postgres Enum"
  [enum-type kw]
  (doto (org.postgresql.util.PGobject.)
    (.setType enum-type)
    (.setValue (name kw))))

(defn as-pg-jsonb
  "Converts a data structure to a JSONB object"
  [data]
  (doto (org.postgresql.util.PGobject.)
    (.setType "JSONB")
    (.setValue (json/generate-string data))))

(defn as-pg-object
  "Converts a data structure to the named object type"
  [object-type data]
  (doto (org.postgresql.util.PGobject.)
    (.setType object-type)
    (.setValue data)))

; So that duct.sql can log sql statements containing PGobject's using erinite.db's JSON logger
(add-encoder org.postgresql.util.PGobject
             (fn [pg-object jsonGenerator]
               (.writeString jsonGenerator (.getValue pg-object))))