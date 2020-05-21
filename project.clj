(defproject erinite-core "0.1.0-SNAPSHOT"
  :description "Base functionality for Erintie application framework"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url "https://github.com/Erinite/erinite-core/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-time "0.15.0"]
                 [org.clojure/tools.reader "1.3.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [cheshire "5.10.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [com.layerware/hugsql-core "0.4.9" :exclusions [org.clojure/tools.reader]]
                 [com.layerware/hugsql-adapter-clojure-java-jdbc "0.4.9"]
                 [samsara/trackit-core "0.9.3"]]
  :repl-options {:init-ns erinite.core})
