# Accessing the App Engine Datastore

In my [last post][0], I managed to deploy a Compojure app to Google
App Engine. Serving static content isn't very exciting, though. Pretty
much every app will need some way to store and retrieve data. So let's
try to access the App Engine Datastore.

<!--more-->

## New Dependencies

To use the datastore API I need to include a jar that comes with the
GAE SDK in my app. I could call the Java API directly, but there are
already a few Clojure libraries that provide friendly wrappers around
it. One of the first (that I know of) was [appengine-clj][1] by John
Hume (who was also one of the first to 
[write about using Clojure on GAE][2]).

I decided to go with [this fork][3] of appengine-clj by Roman Scherer,
which seems to be more complete and actively maintained.  Another
interesting option would be [clj-gae-datastore][4] by the people at
freiheit.com.

I updated my project.clj file with the new dependencies:

```clojure
(defproject compojureongae "0.2.0"
  :description "Example app for deployoing Compojure on Google App Engine"
  :namespaces [compojureongae.core]
  :dependencies [[compojure "0.4.0-RC3"]
                 [ring/ring-servlet "0.2.1"]
                 [hiccup "0.2.4"]
                 [appengine "0.2"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.3.4"]]
  :dev-dependencies [[swank-clojure "1.2.0"]]
  :compile-path "war/WEB-INF/classes"
  :library-path "war/WEB-INF/lib")
```

I'm using [Hiccup][5] (formerly part of Compojure) for HTML
generation. Running `lein deps` runs into an error, because it can't
find the Google SDK jar in the public repositories. Luckily, Leiningen
(or Maven) already tells me how to fix this by installing the jar from
the SDK download into my local Maven repository - I just have to copy
and paste the command from the error message and enter the path to the
local jar. After that, `lein deps` executes cleanly and copies the new
dependencies into my lib dir.

Along with updating the dependencies in project.clj I have to import
the needed symbols into my namespace:

```clojure
(ns compojureongae.core
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use compojure.core
        [ring.util.servlet   :only [defservice]]
        [ring.util.response  :only [redirect]]
        [hiccup.core         :only [h html]]
        [hiccup.page-helpers :only [doctype include-css link-to xhtml-tag]]
        [hiccup.form-helpers :only [form-to text-area text-field]])
  (:import (com.google.appengine.api.datastore Query))
  (:require [compojure.route          :as route]
            [appengine.datastore.core :as ds]))

```

The choice of using `:use` or `:require` is pretty arbitrary in this
case - I just used both to demonstrate the different options. With
`:require` I need to call the functions using the full namespace
(using a short alias), with `:use` they are imported into my
namespace. For the latter case, I explicitly named all the definitions
I need using `:only`. This is pretty verbose and not strictly
necessary, but for a little howto like this I want you to immediately
see where every function comes from, so you don't have to rummage
through all the libraries (although the `doc` function makes this
easy...). I guess it generally is a good idea to not clutter your
namespace with definitions you don't need.

## Storing Data

Now comes the more interesting part: Actually accessing the datastore.
I want to be able to create simple blog posts, consisting of a title
and a body. I need two new routes, one for displaying a form, and one
that is used as the action URL for the form:

```clojure
(defroutes example
  (GET "/" [] (main-page))
  (GET "/new" [] (render-page "New Post" new-form))
  (POST "/post" [title body] (create-post title body))
  (route/not-found "Page not found"))

```

Here's the code that handles the form submission:

```clojure
(defn create-post [title body]
  "Stores a new post in the datastore and issues an HTTP Redirect to the main page."
  (ds/create-entity {:kind "post" :title title :body body})
  (redirect "/"))

```

Amazingly simple. The `create-entity` function just takes a Clojure
map, which needs to have a `:kind` entry, and stores it in the
datastore. After that I issue an HTTP redirect to the main page.

## Retrieving Data

Retrieving data is just as simple. On the main page, I just display all posts:

```clojure
(defn render-post [post]
  "Renders a post to HTML."
  [:div
   [:h2 (h (:title post))]
   [:p (h (:body post))]])

(defn get-posts []
  "Returns all posts stored in the datastore."
  (ds/find-all (Query. "post")))

(defn main-page []
  "Renders the main page by displaying all posts."
  (render-page "Compojure on GAE"
    (map render-post (get-posts))))

```

The `h` function takes care of escaping special characters in the user
input, so I don't run into any cross-site scripting
trouble. `render-page` is a little helper function that takes care of
constructing the common HTML around the payload for all pages.

As usual, the whole code can be found at [Github][6]. The version as
of this writing is [here][7].

## Basic Security

I don't want the whole world to be able to post to my blog, so I need
some authentication and authorization. I could use the App Engine
Users API, but I'll leave that for a later post. Instead I'll go the
simple route and enable security for some URLs in the deployment
descriptor. That way the application itself is blissfully unaware of
it. I just need to add this to the web.xml file:

```xml
  <security-constraint>
    <web-resource-collection>
      <url-pattern>/new</url-pattern>
      <url-pattern>/post</url-pattern>
    </web-resource-collection>
    <auth-constraint>
      <role-name>admin</role-name>
    </auth-constraint>
  </security-constraint>

```

Now only logged-in admin users can post new entries. You can see the
deployed version of the app here:
<http://v0-2.latest.compojureongae.appspot.com/>

## What about the REPL!?

Okay, everything works fine. I can compile the project, start the
dev_appserver to test it locally and deploy it to the Google cloud
(see [my last post][0] for the steps). But what about interactive
development? When I try to call e.g. the `create-entity` function from
a REPL, I only get an Exception. So I can develop and deploy working
software, but I'm back to the dreaded edit-compile-run cycle - that's
not the Clojure way.

I need to fix this, but it'll have to wait until the next post. Sorry...

[0]: /blog/2010/05/11/deploying-to-app-engine
[1]: http://github.com/duelinmarkers/appengine-clj
[2]: http://elhumidor.blogspot.com/2009/04/clojure-on-google-appengine.html
[3]: http://github.com/r0man/appengine-clj
[4]: http://github.com/smartrevolution/clj-gae-datastore
[5]: http://github.com/weavejester/hiccup
[6]: http://github.com/christianberg/compojureongae
[7]: http://github.com/christianberg/compojureongae/tree/v0.2.0
