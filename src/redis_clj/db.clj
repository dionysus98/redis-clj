(ns redis-clj.db)

(defonce !db (atom nil))
;; dev
(defonce !store (atom nil))

(defprotocol KVStore
  (set! [store k v])
  (get! [store k] [store k default]))

(defrecord MemoryStore [!state]
  KVStore
  (set! [_ k v]
    (swap! !state assoc k v))
  (get! [this k]
    (get! this k {:type :bulk-string :value nil}))
  (get! [_ k default]
    (get @!state k default)))

(defn init! [!db]
  (reset! !store (MemoryStore. !db)))

