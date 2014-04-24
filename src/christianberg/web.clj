(ns christianberg.web
  (:require [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis]
            [clj-time.core :as t]
            [clj-time.format :as tf]))

(defn date-from-path [path]
  (tf/parse (tf/formatter "yyyy/MM/dd")
            (second (first (re-seq #"(\d{4}/\d{2}/\d{2})" path)))))

(defn current-year []
  (t/year (t/now)))

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
       [:small
        (str "&copy; 2009-" (current-year) " Christian Berg")]]]]]))

(defn posts []
  "Returns a sequence of all blog posts, where each post is a map."
  (for [[path content] (stasis/slurp-directory "resources/md" #"\.md$")]
    (let [title (second (first (re-seq #"# (.*)" content)))
          first-paragraph-source (second
                                  (first
                                   (re-seq #"(?ms)^# .*?$(.*)<!--more-->"
                                           content)))
          references (first
                      (re-seq #"(?ms)\[\d+\]:.*"
                              content))]
      {:path path
       :url (str/replace path #"\.md$" "")
       :title title
       :date (date-from-path path)
       :source content
       :html (md/to-html content)
       :first-paragraph-source first-paragraph-source
       :references references
       :first-paragraph-snippet (md/to-html (str first-paragraph-source "\n"
                                                 references))})))

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

(defn date-format [date]
  (tf/unparse (tf/formatter "d MMMM yyyy") date))

(defn index-page []
  (layout-page
   [:div.container
    (for [post (take 5 (reverse (sort-by :date (posts))))]
      [:div
       [:h2 [:a {:href (:url post)} (:title post)]]
       [:p.text-muted (date-format (:date post))]
       (:first-paragraph-snippet post)
       [:a.btn.btn-default.btn-xs
        {:href (:url post)}
        "more &raquo;"]
       [:hr]])]))

(defn get-pages []
  (stasis/merge-page-sources
   {:index
    {"/" (index-page)}
    :public
    (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
    :markdown
    (markdown-pages (stasis/slurp-directory "resources/md" #"\.md$"))}))

(def app (stasis/serve-pages get-pages))

(def export-dir "dist")

(defn export []
  (stasis/empty-directory! export-dir)
  (stasis/export-pages (get-pages) export-dir))
