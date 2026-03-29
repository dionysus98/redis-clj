(ns redis-clj.core
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [redis-clj.command :as command]
            [redis-clj.const :as const]
            [redis-clj.db :as db]
            [redis-clj.handler :as handler]
            [redis-clj.resp.decoder :as resp-decoder])
  (:import [java.io BufferedReader BufferedWriter]
           [java.net ServerSocket Socket])
  (:gen-class))

(defn handle-conn!
  [^Socket socket ^clojure.lang.IFn handler]
  (try
    (with-open [^BufferedReader reader (io/reader socket)
                ^BufferedWriter writer (io/writer socket)]
      (log/info :msg "Handling Message")
      (loop []
        (if-let [msg (resp-decoder/decode reader)]
          (do
            (.write writer (handler msg))
            (.flush writer)
            (recur))
          (log/info :msg "client disconnected"))))
    (catch Exception e
      (log/error :error e))
    (finally
      (when-not (.isClosed socket)
        (.close socket)))))

(defonce !server-socket (atom nil))

(defn serve! [port handler]
  ;; == DEV stuff ==
  (when (and (instance? ServerSocket @!server-socket)
             (not (.isClosed @!server-socket)))
    (.close @!server-socket)
    (reset! !server-socket nil))
  ;; == END: DEV stuff ==
  (with-open [^ServerSocket server-sock (reset! !server-socket (ServerSocket. port))]
    (.setReuseAddress server-sock true)
    (while true
      (let [^Socket client-sock (.accept ^Socket server-sock)]
        (future
          (try
            (handle-conn! client-sock handler)
            (catch Exception e
              (println (ex-message e)))))))))

(defn init! [opts]
  (log/info :msg "serving on port: " (:port opts))
  (serve! (:port opts) (partial handler/message-handler (db/init! db/!db))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; You can use print statements as follows for debugging, they'll be visible when running tests.
  #_(log/info "Logs from your program will appear here!")
  ;;[x] Uncomment the code below to pass the first stage
  (let [{:keys [options errors summary]} (command/parse-cli args)]
    #_(println commands)
    (cond
      errors
      (do
        (println errors)
        (println "USAGE: ")
        (println summary))

      (:help options)
      (do
        (println "USAGE: ")
        (println summary))

      (:version options)
      (println "redis_clj 0.0.1")

      :else  (init! options))))

