(defproject clj-heroku-api "0.1.0-SNAPSHOT"
  :description "An always-up-to-date Heroku API client for Clojure"
  :url "http://github.com/dparis/clj-heroku-api"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.1"]
                 [cheshire "5.3.1"]
                 [camel-snake-kebab "0.2.5"]
                 [prismatic/schema "0.3.2"]])
