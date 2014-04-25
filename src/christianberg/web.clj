(ns christianberg.web
  (:require [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.data.xml :as xml]))

(defn date-from-path [path]
  (tf/parse (tf/formatter "yyyy/MM/dd")
            (second (first (re-seq #"(\d{4}/\d{2}/\d{2})" path)))))

(defn slug-from-path [path]
  (second (first (re-seq #"\d{4}/\d{2}/\d{2}/(.*)\.md" path))))

(defn current-year []
  (t/year (t/now)))

(def tracking-code "
  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ga('create', 'UA-21431349-1', 'christianberg.github.io');
  ga('send', 'pageview');
")

(defn layout-page [content]
  (html5
   [:head
    [:meta
     {:name "viewport"
      :content "width=device-width, initial-scale=1"}]
    [:meta
     {:name "author"
      :content "Christian Berg"}]
    [:link
     {:rel "stylesheet"
      :href "//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css"}]
    [:link
     {:rel "alternate"
      :href "/atom.xml"
      :title "Christian Bergs Blog"
      :type "application/atom+xml"}]
    [:title "Christian Bergs Blog"]]
   [:body
    [:div.navbar.navbar-default
     [:div.container
      [:div.navbar-header
       [:a.navbar-brand {:href "/"
                         :title "{:author \"Christian Berg\"\n :type :blog}"}
        "(christianberg.github.io)"]]
      [:a.navbar-text.navbar-right
       {:href "/archive"}
       "Archive"]]]
    content
    [:footer
     [:div.container
      [:p.text-muted
       [:small
        (str "&copy; 2009-" (current-year) " Christian Berg")]]]]
    [:script tracking-code]]))

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
                              content))
          date (date-from-path path)
          slug (slug-from-path path)
          id (str (tf/unparse (tf/formatters :date) date) "-" slug)]
      {:path path
       :url (str/replace path #"\.md$" "")
       :id id
       :slug slug
       :title title
       :date date
       :source content
       :html (md/to-html content [:fenced-code-blocks])
       :first-paragraph-source first-paragraph-source
       :references references
       :first-paragraph-snippet (md/to-html (str first-paragraph-source "\n"
                                                 references))})))

(defn layout-post [content]
  (layout-page
   [:div.container
    content]))

(defn date-format [date]
  (tf/unparse (tf/formatter "d MMMM yyyy") date))

(defn date-short-format [date]
  (.toUpperCase
   (tf/unparse (tf/formatter "d MMM") date)))

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
       [:hr]])
    [:a.btn.btn-default {:href "/archive"} "Blog Archive"]]))

(defn archive-page []
  (let [posts-by-year (group-by #(t/year (:date %)) (posts))]
    (layout-page
     [:div.container
      (for [year (reverse (sort (keys posts-by-year)))]
        [:div.row
         [:div.col-md-12
          [:h2 year]
          (for [post (reverse (sort-by :date (posts-by-year year)))]
            [:div.row
             [:a {:href (:url post)}
              [:div.col-md-1.text-muted.text-right
               (date-short-format (:date post))]
              [:div.col-md-11
               (:title post)]]])]])])))

(defn redirect-to [url]
  (html5
   [:head
    [:link {:rel "canonical"
            :href url}]
    [:meta {:http-equiv "refresh"
            :content (str "0; url=" url)}]]))

(defn- entry [post]
  [:entry
   [:title (:title post)]
   [:updated (:date post)]
   [:author [:name "Christian Berg"]]
   [:link {:href (str "http://christianberg.github.io" (:url post))}]
   [:id (str "urn:christianberg-github-io:feed:post:" (:id post))]
   [:content {:type "html"} (:html post)]])

(defn atom-xml [posts]
  (xml/emit-str
   (xml/sexp-as-element
    [:feed {:xmlns "http://www.w3.org/2005/Atom"}
     [:id "urn:christianberg-github-io:feed"]
     [:updated (-> posts first :date)]
     [:title {:type "text"} "Christian Bergs Blog"]
     [:link {:rel "self" :href "http://christianberg.github.io/atom.xml"}]
     (map entry posts)])))

(defn get-pages []
  (stasis/merge-page-sources
   {:index
    {"/" (index-page)
     "/archive" (archive-page)
     "/blog/archives" (redirect-to "/archive")
     "/atom.xml" (atom-xml (reverse (sort-by :date (posts))))}
    :public
    (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
    :markdown
    (into {}
          (for [post (posts)]
            [(:url post) (layout-post (:html post))]))
    :redirects
    (into {}
          (for [{url :url} (posts)]
            [(str "/blog" url)
             (redirect-to url)]))}))

(def app (stasis/serve-pages get-pages))

(def export-dir "dist")

(defn export []
  (stasis/empty-directory! export-dir)
  (stasis/export-pages (get-pages) export-dir))
