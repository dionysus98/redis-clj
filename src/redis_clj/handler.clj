(ns redis-clj.handler
  (:require
   [clojure.string :as str]
   [redis-clj.db :as db]
   [redis-clj.resp.encoder :as resp-encoder])
  (:import [redis_clj.db KVStore]))

(defmulti message-handler
  (fn message-handler-dispatcher [^KVStore _store msg]
    (:type msg)))

(defmulti command-handler
  (fn command-handler-dispatcher [^KVStore _store command _args]
    (-> command
        (:value)
        (str/trim)
        (str/lower-case)
        (keyword))))

;; === MESSAGE HANDLERS ===

(defmethod message-handler :simple-string [_ {:keys [value]}]
  (case (str/lower-case (str/trim value))
    "ping" (resp-encoder/simple-string "PONG")
    nil))

(defmethod message-handler :array
  [^KVStore store {:keys [value]}]
  (let [[command & args] value]
    (println :msg ":command" command)
    (command-handler store command args)))

;; === COMMAND HANDLERS ===

(defmethod command-handler :ping
  [^KVStore _ _ args]
  (assert (= (count args) 0) "PING takes no args")
  (resp-encoder/simple-string "PONG"))

(defmethod command-handler :echo
  [^KVStore _ _ args]
  (assert (= (count args) 1) "ECHO cannot take more than 1 arg")
  (resp-encoder/bulk-string (:value (first args))))

(defmethod command-handler :command
  [^KVStore _ _ _]
  ;; TODO[x] update this to return all supported commands
  ;; [ref](https://redis.io/docs/latest/commands/command/)
  (resp-encoder/array []))

(defmethod command-handler :set
  [^KVStore store _ args]
  (assert (= (count args) 2) "SET takes 2 args")
  (let [[k v] args]
    ;; keeping `k` as string type for now.
    (db/set! store (:value k) v)
    (resp-encoder/simple-string "OK")))

(defmethod command-handler :get
  [^KVStore store _ args]
  (assert (= (count args) 1) "GET takes 1 arg")
  (let [[k] args]
    ;; keeping `k` as string type for now.
    (resp-encoder/encode (db/get! store (:value k)))))
