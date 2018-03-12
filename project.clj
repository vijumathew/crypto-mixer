(defproject crypto-mixer "0.1"
  :description "mixer for a simple cryptocurrency"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [clj-http "3.8.0"]
                 [cheshire "5.8.0"]
                 [org.clojure/tools.cli "0.3.5"]]
  :main crypto-mixer.core
  :profiles {:uberjar {:aot :all}})

