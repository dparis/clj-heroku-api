(ns clj-heroku-api.core-test
  (:require [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]
            [clj-heroku-api.core :refer :all]
            [schema.core :as s]
            [clj-http.conn-mgr :as http-conn-mgr]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [com.palletops.log-config.timbre :as log-config]
            [clj-http.fake :as http-fake]))

(testable-privates clj-heroku-api.core build-resource-commands)

(def heroku-api-schema-json (slurp (io/resource "heroku-api-schema.json")))
(def heroku-api-schema (json/parse-string heroku-api-schema-json))

(def fake-headers {"Server"                 "nginx/1.4.7"
                   "Content-Type"           "application/schema+json"
                   "X-Content-Type-Options" "nosniff"
                   "X-Runtime"              "0.014656624"
                   "Connection"             "keep-alive"
                   "transfer-encoding"      "chunked"
                   "Status"                 "200 OK"
                   "Date"                   "Thu, 13 Nov 2014 01:42:42 GMT"
                   "Vary"                   "Accept-Encoding"
                   "Request-Id"             "c3d1cb7f-9237-4695-bad8-b1b3e6a0e40c"
                   "Cache-Control"          "public max-age=3600"})

(def fake-resource-data {"test-key" "test-value"})
(def fake-resource-data-json (json/encode fake-resource-data))

(defmacro test-wrapper
  [test]
  `(log-config/suppress-logging
    (s/with-fn-validation
      (http-fake/with-fake-routes
        {"https://api.heroku.com/schema"                (fn [request#] {:status  200
                                                                        :headers fake-headers
                                                                        :body    heroku-api-schema-json})
         "https://api.heroku.com/apps/test-app/dynos/1" (fn [request#] {:status  200
                                                                        :headers fake-headers
                                                                        :body    fake-resource-data-json})}
        ~test))))

(fact-group :core
  (with-state-changes [(around :facts (test-wrapper ?form))]
    (facts "about query-api-schema"
      (let [client (create-client "dummy-auth-token-1234567890XXX")]
        (fact "it returns an api-schema map"
          (query-api-schema client) => heroku-api-schema)))

    (facts "about create-client"
      (let [resource-commands (build-resource-commands heroku-api-schema)
            conn-mgr          (http-conn-mgr/make-reusable-conn-manager {})
            expected-client   {:auth-token        "dummy-auth-token-1234567890XXX"
                               :conn-mgr          conn-mgr
                               :api-schema        heroku-api-schema
                               :resource-commands resource-commands}]
        (fact "it returns a client map"
          (create-client "dummy-auth-token-1234567890XXX") => expected-client
          (provided
            (http-conn-mgr/make-reusable-conn-manager {}) => conn-mgr))))

    (facts "about api-action"
      (let [client            (create-client "dummy-auth-token-1234567890XXX")
            expected-response {:response-data    fake-resource-data
                               :response-raw     fake-resource-data-json
                               :response-status  200
                               :response-headers fake-headers}]
        (fact "it performs an API action and returns an api-response"
          (api-action client "dyno" "info" ["test-app" "1"]) => expected-response)))))
