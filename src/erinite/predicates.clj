(ns erinite.predicates)

(defn contains-all?
  "Returns true if kv contains all expeceted-keys"
  [kv expected-keys]
  (every? #(contains? kv %) expected-keys))
