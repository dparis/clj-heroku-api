(defproject clj-heroku-api "0.1.0-SNAPSHOT"
  :description "An always-up-to-date Heroku API client for Clojure"
  :url "http://github.com/dparis/clj-heroku-api"
  :license {:name "Apache License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]

                 [camel-snake-kebab "0.3.2"]

                 ;; Override version 2.3.1 pulled in by Cheshire
                 ;[com.fasterxml.jackson.core/jackson-core "2.3.2"]

                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [prismatic/schema "1.0.1"]
                 [com.taoensso/timbre "4.1.1"]]

  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies   [[midje "1.7.0"]
                                    [clj-http-fake "1.0.1"]]
                   :plugins        [[lein-midje "3.1.3"]
                                    [codox "0.8.10"]]}}

  :codox {:src-dir-uri               "http://github.com/dparis/clj-heroku-api/blob/master/"
          :src-linenum-anchor-prefix "L"})
