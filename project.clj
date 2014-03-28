(defproject christianberg "0.1.0-SNAPSHOT"
  :description "Source for my blog"
  :url "http://christianberg.github.io/"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [stasis "1.0.0"]
                 [ring "1.2.2"]]
  :ring {:handler christianberg.web/app}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}})
