(ns clj-heroku-api.schemas
  (:require [schema.core :as s])
  (:import [org.apache.http.conn ClientConnectionManager]))

(def HerokuApiSchema
  "A JSON-Schema describing the Heroku platform API."
  ;; NOTE: This schema is very incomplete, currently just checks for
  ;;       ensures the presence and validity of the data required
  ;;       for clj-heroku-api to function.
  {(s/required-key "definitions")
   {s/Str {(s/required-key "stability")   (s/enum "prototype"
                                                  "development"
                                                  "production")
           (s/required-key "links")       [{s/Str s/Any}]
           (s/required-key "title")       s/Str
           (s/required-key "description") s/Str
           (s/optional-key "definitions") {s/Str s/Any}
           s/Str                          s/Any}}

   s/Str s/Any})

(def ResourceCommandProperties
  "A map describing the properties which may be accepted for a
  resource command."
  {s/Str {(s/required-key "required") s/Bool
          s/Str                       s/Any}})

(def ResourceCommands
  "A map describing the commands available for each resource exposed
  through the Heroku platform API."
  {s/Str
   {s/Str {:description                 s/Str
           :method                      (s/enum :delete :get :head
                                                :patch :post :put)
           :path-template               s/Str
           :path-parameters             [s/Str]
           (s/optional-key :properties) ResourceCommandProperties}}})

(def SparseClient
  "Client data used to facilitate connecting to Heroku's platform API in order
  to download the API schema file."
  {:auth-token                         s/Str
   :conn-mgr                           ClientConnectionManager
   (s/optional-key :api-schema)        HerokuApiSchema
   (s/optional-key :resource-commands) ResourceCommands})

(def Client
  "Client data used to facilitate connecting to Heroku's platform API."
  {:auth-token        s/Str
   :conn-mgr          ClientConnectionManager
   :api-schema        HerokuApiSchema
   :resource-commands ResourceCommands})

(def ApiResponse
  "A map containing a response from the Heroku platform API."
  {:response-data    {s/Str s/Any}
   :response-raw     s/Str
   :response-status  s/Int
   :response-headers {s/Str s/Str}})
