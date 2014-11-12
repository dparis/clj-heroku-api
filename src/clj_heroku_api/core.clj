(ns clj-heroku-api.core
  (:require [camel-snake-kebab.core :as csk]
            [cheshire.core :as json]
            [clojure.string :as c-str]
            [clj-http.client :as http]
            [clj-http.conn-mgr :as http-conn-mgr]
            [schema.core :as s]))

(def ^:const heroku-api-address
  "Domain name of Heroku API server. All API requests target this server
  using HTTPS. See Heroku documentation for further details:
  https://devcenter.heroku.com/articles/platform-api-reference#clients."
  "https://api.heroku.com")

(defn- deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins."
  [& vals]
  (let [filtered-vals (remove nil? vals)]
    (if (every? map? filtered-vals)
      (apply merge-with deep-merge filtered-vals)
      (last filtered-vals))))

(defn filter-nil-values
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
  (let [conn-mgr       (:conn-mgr client)
        auth-token     (:auth-token client)
        headers        {"Authorization" (format "Bearer %s" auth-token)
                        "Accept"        "application/vnd.heroku+json; version=3"
                        "Content-Type"  "application/json"}
        request-data   {:connection-manager conn-mgr
                        :method             request-method
                        :url                url
                        :headers            headers}
        response       (http/request (merge request-data opts))]
    (json/parse-string (:body response))))

(defn- url-decode
  [s]
  (java.net.URLDecoder/decode s))

(defn- get-property-from-ref
  [api-schema ref]
  (let [property-path (rest (c-str/split ref #"\/"))]
    (get-in api-schema property-path)))

(defn- get-link-properties-from-schema
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
  [api-schema href]
  (map (comp url-decode second)
       (remove nil? (re-seq #"\{\((.*?)\)?\}" href))))

(defn- href-path
  [href]
  (c-str/replace href #"\{.*?\}" "%s"))

(defn- http-method-keyword
  [method]
  (csk/->kebab-case-keyword method))

(defn- process-link-data
  [api-schema link-data]
  (let [method          (http-method-keyword (get link-data "method"))
        description     (get link-data "description")
        href            (get link-data "href")
        schema          (get link-data "schema")
        command         (csk/->kebab-case (get link-data "title"))
        path-parameters (href-parameters api-schema href)
        path-template   (href-path href)
        property-data   (get-link-properties-from-schema api-schema schema)
        command-data    (filter-nil-values {:description     description
                                            :method          method
                                            :path-template   path-template
                                            :path-parameters path-parameters
                                            :properties      property-data})]
    {command command-data}))

(defn- process-resource
  "Extracts resource data from the API schema based on a resource name."
  [api-schema resource-name]
  (let [resource-data (get-in api-schema ["definitions" resource-name])
        links         (get resource-data "links")]
    (apply merge (map #(process-link-data api-schema %) links))))

(defn- get-resources-from-api-schema
  [api-schema]
  (-> (get api-schema "definitions")
      (keys)
      (sort)))

(defn- build-resource-commands
  [api-schema]
  (let [resources (get-resources-from-api-schema api-schema)]
    (apply merge
           (for [resource resources]
             (let [resource-data (process-resource api-schema resource)]
               {resource resource-data})))))

(defn query-api-schema
  "Queries the Heroku API for API schema data and returns a map of
  response data."
  [client]
  (let [url (build-api-request-url "/schema")]
    (make-api-request client :get url)))

(defn create-client
  "Returns a map of API client data used to make further requests."
  [auth-token]
  (let [conn-mgr          (http-conn-mgr/make-reusable-conn-manager {})
        client            {:auth-token auth-token
                           :conn-mgr   conn-mgr}
        api-schema        (query-api-schema client)
        resource-commands (build-resource-commands api-schema)]
    (assoc client
      :api-schema        api-schema
      :resource-commands resource-commands)))

(defn api-action
  "Performs a Heroku API action and returns the result."
  [client resource command resource-parameters & {:as properties}]
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
    (make-api-request client method url opts)))
