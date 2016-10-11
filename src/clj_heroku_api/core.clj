(ns clj-heroku-api.core
  (:require [camel-snake-kebab.core :as csk]
            [cheshire.core :as json]
            [clojure.string :as c-str]
            [clj-heroku-api.schemas :as schemas]
            [clj-http.client :as http]
            [clj-http.conn-mgr :as http-conn-mgr]
            [schema.core :as s]
            [taoensso.timbre :as log]))


;; ## Vars

(def ^:const heroku-api-address
  "Domain name of Heroku API server. All API requests target this server
  using HTTPS. See Heroku documentation for further details:
  https://devcenter.heroku.com/articles/platform-api-reference#clients."
  "https://api.heroku.com")


;; ## Private Functions

(defn- deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins."
  [& vals]
  (let [filtered-vals (remove nil? vals)]
    (if (every? map? filtered-vals)
      (apply merge-with deep-merge filtered-vals)
      (last filtered-vals))))

(defn- filter-nil-values
  "Returns a map with all keys removed where values are nil."
  [map]
  (into {} (filter (comp not nil? val) map)))

(defn- build-api-request-url
  "Returns a full Heroku API URL string when given a REST resource path."
  [path]
  (str heroku-api-address path))

(defn- make-api-request
  "Returns the result of an HTTP request action targeting a
  Heroku API resource."
  [client request-method url & [opts]]
  (let [conn-mgr      (:conn-mgr client)
        auth-token    (:auth-token client)
        headers       {"Authorization" (format "Bearer %s" auth-token)
                       "Accept"        "application/vnd.heroku+json; version=3"
                       "Content-Type"  "application/json"}
        request-data  {:connection-manager conn-mgr
                       :method             request-method
                       :url                url
                       :headers            headers
                       :throw-exceptions   false}
        http-response (http/request (merge request-data opts))
        response-data {:response-data    (json/parse-string (:body http-response))
                       :response-raw     (:body http-response)
                       :response-status  (:status http-response)
                       :response-headers (:headers http-response)}]
    (log/debug response-data)
    response-data))

(defn- url-decode
  "Decodes a URL-encoded string."
  [s]
  (java.net.URLDecoder/decode s))

(defn- get-property-from-ref
  "Returns a property data map from a JSON-schema $ref string."
  [api-schema ref]
  (let [property-path (rest (c-str/split ref #"\/"))]
    (get-in api-schema property-path)))

(defn- get-link-properties-from-schema
  "Returns a map of link properties from a link data schema."
  [api-schema schema]
  (when schema
    (let [properties     (get schema "properties")
          required-props (set (get schema "required"))]
      (apply merge
             (for [[prop-name prop-data] properties]
               (let [required  (contains? required-props prop-name)
                     prop-data (if (contains? prop-data "$ref")
                                 (get-property-from-ref api-schema
                                                        (get prop-data "$ref"))
                                 prop-data)]
                 {prop-name (assoc prop-data "required" required)}))))))

(defn- href-parameters
  "Returns a sequence of named path parameters extracted from a
  link href string."
  [api-schema href]
  (map (comp url-decode second)
       (remove nil? (re-seq #"\{\((.*?)\)?\}" href))))

(defn- href-path-template
  "Returns a template suitable for use with (format ...), where all named
  path parameters are replaced with %s format specifiers."
  [href]
  (c-str/replace href #"\{.*?\}" "%s"))

(defn- http-method-keyword
  "Converts a string into an HTTP method keyword acceptable for
  use with clj-http."
  [method]
  (csk/->kebab-case-keyword method))

(defn- process-link-data
  "Extracts resource command data from a link-data map."
  [api-schema link-data]
  (when-let [title (get link-data "title")]
    (let [method          (http-method-keyword (get link-data "method"))
          description     (get link-data "description")
          href            (get link-data "href")
          schema          (get link-data "schema")
          command         (csk/->kebab-case title)
          path-parameters (href-parameters api-schema href)
          path-template   (href-path-template href)
          property-data   (get-link-properties-from-schema api-schema schema)
          command-data    (filter-nil-values {:description     description
                                              :method          method
                                              :path-template   path-template
                                              :path-parameters path-parameters
                                              :properties      property-data})]
      {command command-data})))

(defn- process-resource
  "Extracts resource data from the API schema based on a resource name."
  [api-schema resource-name]
  (let [resource-data (get-in api-schema ["definitions" resource-name])
        links         (get resource-data "links")]
    (apply merge (map #(process-link-data api-schema %) links))))

(defn- get-resources-from-api-schema
  "Returns a sorted collection of resources exposed by the API."
  [api-schema]
  (-> (get api-schema "definitions")
      (keys)
      (sort)))

(defn- build-resource-commands
  "Returns a map of resource command data constructed from an api-schema map."
  [api-schema]
  (let [resources (get-resources-from-api-schema api-schema)]
    (apply merge
           (for [resource resources]
             (when-let [resource-data (process-resource api-schema resource)]
               {resource resource-data})))))


;; Heroku API Functions

(s/defn query-api-schema :- schemas/HerokuApiSchema
  "Queries the Heroku API for API schema data and returns a map of
  response data."
  [client :- schemas/SparseClient]
  (let [url          (build-api-request-url "/schema")
        api-response (make-api-request client :get url)]
    (when (= (:response-status api-response) 200)
      (:response-data api-response))))

(s/defn create-client :- schemas/Client
  "Returns a map of API client data used to make further requests."
  [auth-token :- s/Str]
  (let [conn-mgr          (http-conn-mgr/make-reusable-conn-manager {})
        client            {:auth-token auth-token
                           :conn-mgr   conn-mgr}
        api-schema        (query-api-schema client)
        resource-commands (build-resource-commands api-schema)]
    (assoc client
      :api-schema        api-schema
      :resource-commands resource-commands)))

(s/defn api-action :- schemas/ApiResponse
  "Performs a Heroku API action and returns the result."
  ([client resource command resource-parameters]
     (api-action client resource command resource-parameters {}))
  ([client              :- schemas/Client
    resource            :- (s/either s/Str s/Keyword)
    command             :- (s/either s/Str s/Keyword)
    resource-parameters :- [s/Str]
    properties          :- {s/Str s/Any}]
   (let [resource-str      (name resource)
         command-str       (name command)
         resource-commands (:resource-commands client)
         command-data      (get-in resource-commands [resource command])
         method            (:method command-data)
         path-template     (:path-template command-data)
         url-template      (build-api-request-url path-template)
         url               (apply format url-template resource-parameters)
         properties-json   (when-not (empty? properties)
                             (json/encode properties))
         opts              (filter-nil-values {:body properties-json})]
     (make-api-request client method url opts))))
