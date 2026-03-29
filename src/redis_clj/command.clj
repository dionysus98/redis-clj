(ns redis-clj.command
  (:require [clojure.tools.cli :as cli]
            [redis-clj.const :as const]))

(def cli-options
  ;; An option with an argument
  [["-p" "--port PORT" "Port number"
    :default const/PORT
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-v" "--version" "Version"]

   ["-h" "--help" "Print Help"]])

(defn parse-cli [args]
  (cli/parse-opts args cli-options))
