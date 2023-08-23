(ns lrsql.admin.interceptors.jwt
  (:require [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.chain :as chain]
            [lrsql.admin.protocol :as adp]
            [lrsql.util.admin :as admin-u]))

;; NOTE: These interceptors are specifically for JWT validation.
;; For JWT generation see `account/generate-jwt`.

(defn validate-jwt
  "Validate that the header JWT is valid (e.g. not expired)."
  [secret leeway & {:keys [no-val? no-val-uname no-val-issuer]
                    :or   {no-val? false}}]
  (interceptor
   {:name ::validate-jwt
    :enter
    (fn validate-jwt [ctx]
      (let [{lrs :com.yetanalytics/lrs} ctx
            token  (-> ctx
                       (get-in [:request :headers "authorization"])
                       admin-u/header->jwt)
            result (if no-val?
                     (let [{:keys [issuer username]}
                           (admin-u/proxy-jwt->username-and-issuer
                            token no-val-uname no-val-issuer)]
                       (adp/-ensure-account-oidc lrs username issuer))
                     (admin-u/jwt->account-id token secret leeway))]
        (cond
          ;; Success - assoc the account ID as an intermediate value
          (uuid? result)
          (-> ctx
              (assoc-in [::data :account-id] result)
              (assoc-in [:request :session ::data :account-id] result))
          ;; Problem with the non-validated account ensure
          (= :lrsql.admin/oidc-issuer-mismatch-error result)
          (assoc (chain/terminate ctx)
                 :response
                 {:status 401
                  :body   {:error "OIDC Issuer Mismatch!"}})
          ;; The token is bad (expired, malformed, etc.) - Unauthorized
          (= :lrsql.admin/unauthorized-token-error result)
          (assoc (chain/terminate ctx)
                 :response
                 {:status 401
                  :body   {:error "Unauthorized JSON Web Token!"}}))))}))

(def validate-jwt-account
  "Check that the account ID stored in the JWT exists in the account table.
   This should go after `validate-jwt`, and MUST be present if `account-id`
   is used in the route (e.g. for credential operations)."
  (interceptor
   {:name ::validate-jwt-account
    :enter
    (fn validate-jwt-account [ctx]
      (let [{lrs :com.yetanalytics/lrs
             {:keys [account-id]} ::data} ctx]
        (if (adp/-existing-account? lrs account-id)
          ;; Success - continue on your merry way
          ctx
          ;; The account does not exist/was deleted - Unauthorized
          (assoc (chain/terminate ctx)
                 :response
                 {:status 401
                  :body   {:error "Unauthorized, account does not exist!"}}))))}))
