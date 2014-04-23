(ns christianberg.web
  (:require [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis])
  (:import [java.util Calendar]))

(defn current-year []
  (.. Calendar
      getInstance
      (get Calendar/YEAR)))

(defn layout-page [content]
  (html5
   [:head
    [:meta
     {:name "viewport"
      :content "width=device-width, initial-scale=1"}]
    [:link
     {:rel "stylesheet"
      :href "//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css"}]
    [:title "Christian Bergs Blog"]]
   [:body
    [:div.navbar.navbar-default
     [:div.container
      [:div.navbar-header
       [:a.navbar-brand {:href "/"}
        "christianberg.github.io"]]]]
    content
    [:footer
     [:div.container
      [:p.text-muted
       [:small (str "&copy; 2009-" (current-year) " Christian Berg")]]]]]))

(defn layout-post [content]
  (layout-page
   [:div.container
    content]))

(defn markdown-pages [pages]
  (into {}
        (for [[path content] pages]
          (let [stripped-path (str/replace path #"\.md$" "")]
            [stripped-path
             (layout-post (md/to-html content))]))))

(defn get-pages []
  (stasis/merge-page-sources
   {:public
    (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
    :markdown
    (markdown-pages (stasis/slurp-directory "resources/md" #"\.md$"))}))

(def app (stasis/serve-pages get-pages))
