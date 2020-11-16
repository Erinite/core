(ns erinite.utils
  #?(:clj (:require [clj-time.core :as time]
                    [clj-time.coerce :as timec]
                    [clojure.java.io :as io]
                    [taoensso.nippy :as nippy]))
  #?(:clj (:import [java.util Base64])))

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
   (defn base64-encode
     [bytes]
     (.encodeToString (Base64/getEncoder) bytes)))

#?(:clj
   (defn base64-decode
     [bytes]
     (.decode (Base64/getDecoder) bytes)))