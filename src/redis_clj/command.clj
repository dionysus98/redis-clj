(ns redis-clj.command
  (:require [clojure.tools.cli :as cli]
            [redis-clj.const :as const]
            [clojure.string :as str]))

(def cli-options
  ;; An option with an argument
  [["-p" "--port PORT" "Port number"
    :default const/PORT
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

   [nil "--replicaof" "Start a replica server on <MASTER_HOST> <MASTER_PORT>"
    :default (str "localhost " const/PORT)
    :parse-fn (fn [s]
                (-> (str/trim s)
                    (str/split #" ")
                    (as-> [master-host master-port]
                          {:master/host master-host
                           :master/port (Integer/parseInt master-port)})))
    :validate [#(and (string? (:master/host %))
                     (number? (:master/port %))) "Must be <MASTER_HOST> <MASTER_PORT>"]]

   ["-v" "--version" "Version"]

   ["-h" "--help" "Print Help"]])

(defn parse-cli [args]
  (cli/parse-opts args cli-options))