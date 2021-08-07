(ns lrsql.util.authority
  "Utilities for generating xAPI authority Agents"
  (:require [cheshire.core :as json]
            [clojure.core.memoize :as mem]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [lrsql.spec.common :as c]
            [selmer.parser :as selm-parser]
            [selmer.util :as selm-u]
            [xapi-schema.spec :as xs])
  (:import [java.io File]))

(s/def ::authority-url string?)
(s/def ::cred-id ::c/primary-key)
(s/def ::account-id ::c/primary-key)

(s/def ::context-map
  (s/keys :req-un [::authority-url
                   ::cred-id
                   ::account-id]))

(defn throw-on-missing
  "When a user enters a variable and it is not in our context map, throw!
   Used by selmer when context map validation fails."
  [tag context-map]
  (throw (ex-info
          (format "\"%s\" is not a valid variable for the authority template."
                  (:tag-value tag))
          {:type        ::unknown-variable
           :tag         tag
           :context-map context-map})))

(def sample-authority-fn-input
  {:authority-url "https://lrs.example.com"
   :cred-id       #uuid "00000000-0000-4000-0000-000000000001"
   :account-id    #uuid "00000000-0000-4000-0000-000000000002"})

(defn- valid-authority-fn?
  [authority-fn]
  (s/valid? ::xs/agent (authority-fn sample-authority-fn-input)))

(s/fdef make-authority-fn
  :args (s/cat :template-path (s/nilable string?) :threshold (s/? pos-int?))
  :ret (s/fspec
        :args (s/cat :context-map ::context-map)
        :ret ::xs/agent))

(defn make-authority-fn**
  "Returns a function that will render the template to data, using `template`.
   Not memoized."
  [template]
  (fn [context-map]
    (binding [selm-u/*missing-value-formatter* throw-on-missing
              selm-u/*filter-missing-values*   (constantly false)]
      (-> template
          (selm-parser/render-template context-map)
          json/parse-string))))

(defn make-authority-fn*
  "Returns a function that will render the template to data, using the
   template read at `template-path`. Not memoized."
  [template-path]
  (let [^File f (io/file template-path)]
    (if (and f (.exists f))
      (let [template     (selm-parser/parse* f)
            authority-fn (make-authority-fn** template)]
        (if (valid-authority-fn? authority-fn)
          authority-fn
          (throw
           (ex-info "Authority template does not produce a valid xAPI Agent"
                    {:type          ::invalid-json
                     :template-path template-path}))))
      (throw
       (ex-info (format "No authority template specified at %s" template-path)
                {:type          ::no-authority-template
                 :template-path template-path})))))

(defn make-authority-fn
  "Returns a memoized function that will render the template to data.
   Will use the template at the filesystem path `template-path`; throws an
   exception if the template is not found or if it is malformed.
   The `threshold` optional arg controls the how many authorities will be
   cached before least-recently used (LRU) clearing."
  [template-path & [threshold]]
  (mem/lru (make-authority-fn* template-path)
           :lru/threshold (or threshold 512)))

(comment

  ;; use the default by resource path
  (let [a-fn (make-authority-fn nil)]
    (a-fn {:authority-url "https://lrs.example.com"
           :cred-id       #uuid "41ec697d-802e-4f3e-aad5-e5fc9fb55f35"
           :account-id    #uuid "3aa61cf9-a697-46f1-b60d-62a2c78ab33b"}))

  ;; use arbitrary by fs path
  (let [a-fn (make-authority-fn "config/authority.json.template")]
    (a-fn {:authority-url "https://lrs.example.com"
           :cred-id       #uuid "41ec697d-802e-4f3e-aad5-e5fc9fb55f35"
           :account-id    #uuid "3aa61cf9-a697-46f1-b60d-62a2c78ab33b"}))
  ;; only remember one authority ;; don't ever do this but just for example
  (let [a-fn (make-authority-fn "config/authority.json.template" 1)]
    (a-fn {:authority-url "https://lrs.example.com"
           :cred-id       #uuid "41ec697d-802e-4f3e-aad5-e5fc9fb55f36"
           :account-id    #uuid "3aa61cf9-a697-46f1-b60d-62a2c78ab33b"}))

  ;; Run `make-authority-fn` during startup and then pass the resulting function
  ;; to a suitable component. It will only ever render the agent once for a
  ;; given context map (input), until old entries are evicted when `threshold`
  ;; is reached
  )
