(defproject christianberg "0.1.0-SNAPSHOT"
  :description "Source for my blog"
  :url "http://christianberg.github.io/"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [stasis "2.2.1"]
                 [ring "1.3.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]
                 [clj-time "0.8.0"]
                 [org.clojure/data.xml "0.0.8"]]
  :ring {:handler christianberg.web/app}
  :aliases {"build-site" ["run" "-m" christianberg.web/export]}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}})
