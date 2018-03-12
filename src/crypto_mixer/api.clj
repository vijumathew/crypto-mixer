(ns crypto-mixer.api
  (:require [clj-http.client :as h]
            [clojure.core.async :as a]
            [cheshire.core :as c]
            [cheshire.parse :as p]
            [clojure.spec.alpha :as s]
            [crypto-mixer.util :as u]
            [clojure.string :as str]))

(defonce ^:private jobcoin-api "http://jobcoin.gemini.com/tamer/api/")
(defonce ^:private divider "/")
(defonce ^:private address-suffix "addresses")
(defonce ^:private transactions-suffix "transactions")

;;specs
(s/def ::balance (s/conformer u/number-conform?))
(s/def ::amount (s/conformer u/number-conform?))
(s/def ::fromAddress string?)
(s/def ::toAddress string?)

(s/def ::transaction (s/keys :req-un [::toAddress ::amount]
                             :opt-un [::fromAddress]))
(s/def ::transactions (s/coll-of ::transaction))
(s/def ::transactions-response (s/keys :req-un [::balance ::transactions]))

;;api calls
(defn- body-to-json [res]
  (c/decode (:body res) true))

(defn- address-api-internal [addr cb err]
  (h/get (str jobcoin-api address-suffix divider addr)
         {:async? true}
         (comp cb body-to-json)
         err))

(defn- transactions-api-internal [cb err]
  (h/get (str jobcoin-api transactions-suffix)
         {:async? true}
         (comp cb body-to-json)
         err))

;;public API calls
(defn transactions-post [from to amount cb err]
  (h/post (str jobcoin-api transactions-suffix)
          {:async? true
           :form-params
           {:fromAddress from
            :toAddress to
            :amount (str amount)}}
          cb err))

(defn address-get-blocking
  [addr]
  (->> addr
       (str jobcoin-api address-suffix divider)
       h/get
       body-to-json
       (s/conform ::transactions-response)))

(defn address-get [addr cb err]
  (address-api-internal
   addr
   (comp cb (partial s/conform ::transactions-response))
   err))

(defn transactions-get [cb err]
  (transactions-api-internal
   (comp cb (partial s/conform ::transactions))
   err))
