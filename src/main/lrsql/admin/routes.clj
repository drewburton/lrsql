(ns lrsql.admin.routes
  (:require [clojure.set :as cset]
            [com.yetanalytics.lrs.pedestal.interceptor :as i]
            [lrsql.admin.interceptors :as li]))

(def admin-account-routes
  #{;; Create new account
    ["/admin/account/create" :post (conj i/common-interceptors
                                         li/create-admin
                                         li/generate-jwt)]
     ;; Log into an existing account
    ["/admin/account/login" :post (conj i/common-interceptors
                                        li/authenticate-admin
                                        li/generate-jwt)]
     ;; Delete account (and associated tokens)
    ["/admin/account" :delete (conj i/common-interceptors
                                    li/authenticate-admin
                                    li/delete-admin)]})

(def admin-token-routes
  #{;; Create new tokens w/ scope
    ["/admin/token" :put (conj i/common-interceptors
                               li/validate-jwt
                               li/create-api-keys)]
     ;; Create or update new tokens w/ scope
    ["/admin/token" :post (conj i/common-interceptors
                                li/validate-jwt
                                li/update-api-keys)]
     ;; Get current tokens + scope associated w/ account
    ["/admin/token" :get (conj i/common-interceptors
                               li/validate-jwt
                               li/read-api-keys)]
     ;; Delete tokens
    ["/admin/token" :delete (conj i/common-interceptors
                                  li/validate-jwt
                                  li/delete-api-keys)]})

;; TODO: Add additional interceptors
(defn add-admin-routes
  "Given a set of routes `routes` for a default LRS implementation,
   add additional routes specific to creating and updating admin
   accounts."
  [routes]
  (cset/union routes admin-account-routes admin-token-routes))
