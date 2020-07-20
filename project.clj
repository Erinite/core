(defproject erinite/core "0.1.0-SNAPSHOT"
  :description "Base functionality for Erintie application framework"
  :url "https://github.com/Erinite/core"
  :license {:name "MIT"
            :url "https://github.com/Erinite/erinite-core/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-time "0.15.0"]
                 [org.clojure/tools.reader "1.3.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [cheshire "5.10.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [com.layerware/hugsql "0.5.1" :exclusions [org.clojure/tools.reader]]
                 [ring-cors "0.1.13"]
                 [nano-id "1.0.0"]
                 [jstrutz/hashids "1.0.1"]]
  :repl-options {:init-ns erinite.core})
