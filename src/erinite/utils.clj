(ns erinite.utils
  (:require [clj-time.core :as time]
            [clj-time.coerce :as timec]
            [clojure.java.io :as io]
            [taoensso.nippy :as nippy]))

(defn get-current-time []
  (timec/to-long (time/now)))

(defn get-current-sql-time []
  (timec/to-sql-time (time/now)))

(defn thaw
  [byte-object]
  (when byte-object
    (with-open [xout (java.io.ByteArrayOutputStream.)]
      (io/copy byte-object xout)
      (nippy/thaw (.toByteArray xout)))))

(defn freeze
  [data]
  (when data
    (nippy/freeze data)))

