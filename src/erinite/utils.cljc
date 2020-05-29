(ns erinite.utils
  #?(:clj (:require [clj-time.core :as time]
                    [clj-time.coerce :as timec]
                    [clojure.java.io :as io]
                    [taoensso.nippy :as nippy])))

#?(:clj
   (defn get-current-time []
     (timec/to-long (time/now))))

#?(:clj
   (defn get-current-sql-time []
     (timec/to-sql-time (time/now))))

#?(:clj
   (defn thaw
     [byte-object]
     (when byte-object
       (with-open [xout (java.io.ByteArrayOutputStream.)]
         (io/copy byte-object xout)
         (nippy/thaw (.toByteArray xout))))))

#?(:clj
   (defn freeze
     [data]
     (when data
       (nippy/freeze data))))

(defn when-fn
  "Apply f only if argument is not nil, otherwise return nil"
  [f]
  #(when-not (nil? %) (f %)))

(defn if-fn
  "Apply f only if argument is not nil, otherwise return v. Opposite of fnil"
  [f v]
  #(if (nil? %) v (f %)))
