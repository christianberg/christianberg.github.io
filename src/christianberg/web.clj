(ns christianberg.web
  (:require [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis]))

(defn layout-page [content]
  (html5
   [:head
    [:title "Christian Bergs Blog"]]
   [:body content]))

(defn markdown-pages [pages]
  (zipmap (map #(str/replace % #"\.md$" "") (keys pages))
          (map #(layout-page (md/to-html %)) (vals pages))))

(defn get-pages []
  (stasis/merge-page-sources
   {:public
    (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
    :markdown
    (markdown-pages (stasis/slurp-directory "resources/md" #"\.md$"))}))

(def app (stasis/serve-pages get-pages))
