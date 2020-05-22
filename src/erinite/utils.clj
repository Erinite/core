(ns erinite.utils
  (:require [clj-time.core :as time]
            [clj-time.coerce :as timec]
            [clojure.java.io :as io]
            [taoensso.nippy :as nippy]
            [nano-id.core :as nano-id]))

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

(defn when-fn
  "Apply f only if argument is not nil, otherwise return nil"
  [f]
  #(when-not (nil? %) (f %)))

(defn if-fn
  "Apply f only if argument is not nil, otherwise return v. Opposite of fnil"
  [f v]
  #(if (nil? %) v (f %)))

(def make-id (nano-id/custom "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890" 15))