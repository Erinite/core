(ns erinite.json
  (:require [jsonista.core :as j]))

(def mapper
  (j/object-mapper
   {:encode-key-fn name
    :decode-key-fn keyword}))

(defn generate-string
  [obj]
  (j/write-value-as-string obj mapper))

(defn parse-string
  ([obj]
   (parse-string obj true))
  ([obj keywordize?]
   (if keywordize?
     (j/read-value obj mapper)
     (j/read-value obj))))
