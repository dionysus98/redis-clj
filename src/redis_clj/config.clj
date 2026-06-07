(ns redis-clj.config
  (:gen-class))


(deftype ServerConfig [port replicaof])

(defn >server-config ^ServerConfig [opts]
  (ServerConfig. (:port opts) (:replicaof opts)))