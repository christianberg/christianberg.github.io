(defproject christianberg "0.1.0-SNAPSHOT"
  :description "Source for my blog"
  :url "http://christianberg.github.io/"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [stasis "1.0.0"]
                 [ring "1.2.2"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]
                 [clj-time "0.6.0"]]
  :ring {:handler christianberg.web/app}
  :aliases {"build-site" ["run" "-m" christianberg.web/export]}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}})
