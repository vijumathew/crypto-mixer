(ns crypto-mixer.core
  (:require [crypto-mixer.transactions :refer
             [get-new-addr-blocking split-transfer-to-addrs]]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  [["-i" "--interval INTERVAL" "Transactions spread over INTERVAL (sec)"
    :default 1
    :parse-fn #(Integer/parseInt %)]
   ["-a" "--addresses ADDRESS1 ADDRESS2..." "list of addresses"
    :assoc-fn (fn [m k v] (update-in m [k] conj v))]])

(defn -main
  "takes command line arguments of the form:
  -i INTERVAL -a ADDRESS_1 -a ADDRESS_2 -a ...

  waits for a transfer to the printed address and
  then transfers that amount randomly to the provided
  addresses. might deduct a fee as well."
  [& args]
  (let [{options :options} (parse-opts args cli-options)
        addr (get-new-addr-blocking)
        res (promise)]
    (println "Send jobcoins to"
             addr
             "- listening for 5 minutes...")
    (split-transfer-to-addrs addr
                             (:interval options)
                             (:addresses options)
                             #(deliver res %))
    (if (= true @res)
      (println "Success!")
      (println "Error"
       (when (string? @res) @res)))))
