(ns crypto-mixer.util)

(defonce ^:private transaction-sizes [0.1 0.2 0.5 1 1.5 2 2.5 5 10 20])

(defn number-conform?
  "parse string into Integer or Double"
  [x]
  (if (integer? x)
    x
    (if (string? x)
      (try
        (if (clojure.string/includes? x ".")
          (Double/parseDouble x) (Integer/parseInt x))
        (catch Exception e
          :clojure.spec/invalid)))))

(defn make-unique-id
  "return 8 digit unique id"
  ([] (make-unique-id #{}))
  ([ids]
   (letfn [(new-id []
             (-> (java.util.UUID/randomUUID)
                 str
                 (subs 0 8)))]
     (loop [id (new-id)]
       (if (contains? ids id)
         (recur (new-id))
         id)))))

(defn get-second-rounding
  "return amount of seconds until next minute"
  []
  (let [now (java.time.Instant/now)
        next-min (-> now
                     (.plusSeconds 60)
                     (.truncatedTo
                      java.time.temporal.ChronoUnit/MINUTES))]
    (.until now next-min
            java.time.temporal.ChronoUnit/SECONDS)))

(defn split-amt
  "splits AMOUNT into random pieces of 
  sizes equal to TRANSACTION-SIZES."
  [amount]
  (let [n (count transaction-sizes)]
    (loop [amt amount
           idx (rand-int n)
           out '()]
      (let [t (nth transaction-sizes idx)]
        (cond
          (= t amt) (conj out t)
          (> t amt)
          (if (> (nth transaction-sizes 2) amt)
            out
            (recur amt (rand-int (dec idx)) out))
          (< t amt)
          (recur (- amt t) (rand-int n) (conj out t))
          :else out)))))
