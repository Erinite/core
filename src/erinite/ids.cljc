(ns erinite.ids
  (:require [nano-id.core :as nano-id]
            [hashids.core :as hashids]))

(def alphabet "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890")

(def -make-generator (memoize (fn [size] (nano-id/custom alphabet size))))

(defn make-id
  "Generate a random string of `size` characters from alphabet a-zA-Z0-9 (base 62). `size` defaults to 15"
  [& [size]]
  ((-make-generator (or size 15))))

(defn encode
  "Encode `id` with alphabet a-zA-Z0-9 (base 62) or custom alphabet"
  ([id]
   (hashids/encode {:alphabet alphabet} id))
  ([opts id]
   (hashids/encode (update opts :alphabet (fnil identity alphabet)) id)))

(defn decode
  "Decodes a string encoded with `encode`, must use identical `opts`"
  ([id]
   (first (hashids/decode {:alphabet alphabet} id)))
  ([opts id]
   (first (hashids/decode (update opts :alphabet (fnil identity alphabet)) id))))
