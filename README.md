# clj-heroku-api

An always-up-to-date Heroku API client for Clojure.

The Heroku platform API provides a [JSON-schema](http://json-schema.org/) describing
the resources exposed through the API and any commands which can be performed against those
resources. The clj-heroku-api library downloads and caches this schema when creating a client
connection and uses the schema to construct REST resource paths and request bodies.

**NOTE:** This library is still in early development, and subject to change at any time.
I intend to follow SemVer/[BreakVer](https://github.com/ptaoussanis/encore/blob/master/BREAK-VERSIONING.md)
versioning semantics, so prior to v1.0.0, watch out for breaking changes!


## Usage

```clojure
(def client (create-client "heroku-api-auth-token"))

(api-action client "apps" "create"
  []
  {"name"   "test-app"
   "region" "us"
   "stack"  "cedar"})

(api-action client "dyno" "create"
  ["test-app"]
  {"size" "2X"})

```


## Todo

* Parameter/property validation based on API schema
* Macros to auto-generate API namespaces/functions for each resource/command
* Cleaner support for header-based features
  * [Caching](https://devcenter.heroku.com/articles/platform-api-reference#caching)
  * [Data Integrity](https://devcenter.heroku.com/articles/platform-api-reference#data-integrity)
  * [Response Ranges](https://devcenter.heroku.com/articles/platform-api-reference#ranges)


## Thanks

Development and release of clj-heroku-api was generously supported by [Revcaster](https://revcaster.com/)


## License

Copyright © 2014 Dylan Paris

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
