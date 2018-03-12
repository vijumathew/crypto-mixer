(ns crypto-mixer.transactions
   (:require [clojure.core.async :as a]
             [crypto-mixer.api :as api]
             [crypto-mixer.util :as u]))

(defonce ^:private house-addr "house-address")
(defonce ^:private timeout-length (* 20 1000))
(def count-transactions (comp count :transactions))

(defn get-new-addr-blocking
  "blocking get for new-address. 
  this finds an unused address to recieve
  the initial transfer amount for mixing."
  []
  (let [res-chan (a/chan)
        err-chan (a/chan)
        fetch (fn [addr]
                (api/address-get-blocking addr))]
    (loop [addr (u/make-unique-id)]
      (let [res (fetch addr)]
        (if (= (count-transactions res) 0)
          addr
          (do (fetch) (recur (u/make-unique-id))))))))

(defn- wait-on-addr
  "waits for a transfer to come in to
  ADDR. this polls every 5 seconds for 5 minutes. 
  passes entire transactions-response to CB"
  [addr cb]
  (let [res-chan (a/chan)
        err-chan (a/chan)
        amt (atom nil)
        fetch (fn []
                (api/address-get addr
                                 #(a/put! res-chan %)
                                 #(a/put! err-chan %)))]
    (a/go
      (fetch)
      (reset! amt (count-transactions (a/<! res-chan)))
      (loop [iter 0]
        (fetch)
        (let [new-res (a/<! res-chan)]
          (if (> (count-transactions new-res) @amt)
            (cb new-res)
            (if (> iter 60)
              (cb ::no-result)
              (do
                (a/<! (a/timeout 5000))
                (recur (inc iter))))))))))

(defn- transfer-transaction-to-house
  "transfer transaction to house. does not retry"
  [t cb]
  (let [amt (:amount t)
        addr (:toAddress t)]
    (api/transactions-post addr house-addr amt cb
                           (fn [_] (cb ::error)))))

(defn- generate-transfer-schedule
  "takes a TRANSACTION and returns a schedule of 
  timeouts and amounts to construct future transactions.
  timeouts have a max duration of INTERVAL (secs).

  A fee might be deducted - it just won't be transferred 
  to the final address."
  ([transaction] (generate-transfer-schedule transaction 1))
  ([{amount :amount} interval]
   (let [ival (* 1000 interval)]
     (map #(assoc {} :amt %1 :time %2)
          (u/split-amt amount) (repeatedly (partial rand-int ival))))))

(defn- post-transfer [res-chan err-chan addresses evt]  
  (api/transactions-post house-addr
                         (rand-nth addresses)
                         (:amt evt)
                         #(a/put! res-chan %)
                         #(a/put! err-chan [evt %])))

(defn- schedule-transfers
  "schedules transfers detailed in SCHEDULE 
  and calls CB when transfers complete.
  transfers go from house-addr to random
  address in ADDRESSES.

  schedules transfers starting at next minute
  for increased privacy.

  if a transfer fails it will be retried indefinitely."
  [schedule addresses cb]
  (let [schedule-chan (a/chan)
        res-chan (a/chan)
        err-chan (a/chan)
        transfer (partial post-transfer res-chan err-chan addresses)]
    (println "scheduling...")
    (doseq [evt schedule]
      (a/go
        (a/<! (a/timeout (+ 0 (* 1000 (u/get-second-rounding))
                            (:time evt))))
        (transfer evt)))
    (a/go
      (loop [transfers-completed 0]
        (if (= transfers-completed (count schedule))
          (cb true)
          (do (a/<! res-chan)
              (recur (inc transfers-completed))))))
    (a/go
      (loop []
        (let [[evt _] (a/<! err-chan)]
          (transfer evt)
          (recur))))))

(defn split-transfer-to-addrs
  "Wires all the pieces together. 

  Waits on ADDR to get an initial transfer,
  then sends this amount to house-addr.
  When that has completed, this schedules
  randomized transactions with max-length
  INTERVAL to ADDRESSES using 
  `schedule-transfers`.

  Calls CB when complete."
  [addr interval addresses cb]
  (let [m-chan (a/chan)]
    (a/go
      (wait-on-addr addr #(a/put! m-chan %))
      (let [res (a/<! m-chan)]
        (if (= res ::no-result)
          (cb (str "no transaction detected at: " addr))
          (let [l (last (:transactions res))]
            (transfer-transaction-to-house l #(a/put! m-chan %))
            (if (= (a/<! m-chan) ::error)
              (cb "transfer to house failed")
              (schedule-transfers
               (generate-transfer-schedule l interval)
               addresses
               cb))))))))
