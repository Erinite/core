(ns erinite.config
  (:require [integrant.core :as ig]))

;; Allows static data to be given a name so that it can be referenced
;; by multiple components
(defmethod ig/init-key ::static
  [_ options]
  options)
