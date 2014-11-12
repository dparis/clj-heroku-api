(defproject clj-heroku-api "0.1.0-SNAPSHOT"
  :description "An always-up-to-date Heroku API client for Clojure"
  :url "http://github.com/dparis/clj-heroku-api"
  :license {:name "Apache License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]

                 ;; Exclude org.clojure/clojure due to version range includes
                 ;; in camel-snake-kebab deps
                 [camel-snake-kebab "0.2.5" :exclusions [org.clojure/clojure]]

                 ;; Override version 2.3.1 pulled in by Cheshire
                 [com.fasterxml.jackson.core/jackson-core "2.3.2"]

                 [cheshire "5.3.1"]
                 [clj-http "1.0.1"]
                 [prismatic/schema "0.3.3"]
                 [com.taoensso/timbre "3.3.1"]]

  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies   [[midje "1.6.3"]
                                    [clj-http-fake "0.7.8"]
                                    [com.palletops/log-config "0.1.4"]]
                   :plugins        [[lein-midje "3.1.3"]
                                    [codox "0.8.10"]]}}

  :codox {:src-dir-uri               "http://github.com/dparis/clj-heroku-api/blob/master/"
          :src-linenum-anchor-prefix "L"})
