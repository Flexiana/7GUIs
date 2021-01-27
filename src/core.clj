(ns core
  (:gen-class)
  (:require
   [io.pedestal.http :as http]
   [clojure.java.io :as io]
   [shadow.cljs.devtools.api :as shadow]))

(def ^:private idx-html (slurp (io/resource "production/index.html")))

(defn index [_]
  {:status  200
   :body    idx-html
   :headers {"Content-Type" "text/html"}})

(def routes
  #{["/" :get index :route-name ::index]})

(def config-map
  (-> {::http/type   :jetty
       ::http/resource-path "production"
       ::http/file-path "production/js"
       ::http/secure-headers nil ;;{:content-security-policy-settings {:object-src "none"}}
       ::http/port   3000
       ::http/routes routes
       ::http/join?  false}
      http/default-interceptors
      http/create-server))

(defonce server (atom nil))

(defn release-prod [& _]
  (shadow/release :sguis))

(defn -main
  [& _]
  (swap! server
         (fn [st]
           (some-> st http/stop)
           (-> config-map
               http/create-server
               http/start))))
