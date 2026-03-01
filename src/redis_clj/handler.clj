(ns redis-clj.handler
  (:require
   [clojure.string :as str]
   [redis-clj.resp.encoder :as resp-encoder]))

(defmulti message-handler :type)

(defmulti command-handler
  (fn [command _args]
    (-> command
        (:value)
        (str/trim)
        (str/lower-case)
        (keyword))))

;; === MESSAGE HANDLERS ===

(defmethod message-handler :simple-string [{:keys [value]}]
  (case (str/lower-case (str/trim value))
    "ping" (resp-encoder/simple-string "PONG")
    nil))

(defmethod message-handler :array [{:keys [value]}]
  (let [[command & args] value]
    (command-handler command args)))

;; === COMMAND HANDLERS ===

(defmethod command-handler :ping [_ args]
  (assert (= (count args) 0) "PING takes no args")
  (resp-encoder/simple-string "PONG"))

(defmethod command-handler :echo [_ args]
  (assert (= (count args) 1) "ECHO cannot take more than 1 arg")
  (resp-encoder/bulk-string (:value (first args))))

