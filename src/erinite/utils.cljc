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

#?(:clj
   (defn input-stream->str
     [input-stream]
     (when input-stream
       (with-open [xout (java.io.ByteArrayOutputStream.)]
         (io/copy input-stream xout)
         (.toString xout)))))
