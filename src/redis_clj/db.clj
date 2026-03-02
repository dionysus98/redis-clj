(ns redis-clj.db)

(defonce !db (atom nil))
;; dev
(defonce !store (atom nil))

(defprotocol KVStore
  (put! [store k v] [store k v opts])
  (get! [store k] [store k default])
  (del! [store k]))

(defrecord MemoryStore [!state]
  KVStore

  (put! [this k v]
    (put! this k v {}))
  (put! [_ k v opts]
    (swap! !state assoc k
           (cond-> v
             (number? (:ttl opts))
             (assoc :exp (+ (:ttl opts) (System/currentTimeMillis))))))

  (get! [this k]
    (get! this k {:type :bulk-string :value nil}))
  (get! [this k default]
    (let [v (get @!state k default)
          has-exp? (number? (:exp v))]
      (cond
        ;; not expired yet.
        (and has-exp? (> (:exp v) (System/currentTimeMillis))) v
        ;; expired.
        has-exp? (do (del! this k) default)
        ;; no timer logic
        :else v)))

  (del! [_ k]
    (swap! !state dissoc k)))

(defn init! [!db]
  (reset! !store (MemoryStore. !db)))

