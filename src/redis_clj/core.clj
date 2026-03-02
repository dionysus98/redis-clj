(ns redis-clj.core
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [redis-clj.db :as db]
            [redis-clj.handler :as handler]
            [redis-clj.resp.decoder :as resp-decoder])
  (:import [java.io BufferedReader BufferedWriter]
           [java.net ServerSocket Socket])
  (:gen-class))

(def ^:const PORT 6379)

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
  (let [^ServerSocket server-sock (reset! !server-socket (ServerSocket. port))]
    (.setReuseAddress server-sock true)
    (while true
      (let [^Socket client-sock (.accept ^Socket server-sock)]
        (future (handle-conn! client-sock handler))))))

(defn init! [& _]
  (log/info :msg "serving on port: " PORT)
  (serve! PORT (partial handler/message-handler (db/init! db/!db))))

(defn -main
  "I don't do a whole lot ... yet."
  [& _]
  ;; You can use print statements as follows for debugging, they'll be visible when running tests.
  (log/info "Logs from your program will appear here!")
  ;;[x] Uncomment the code below to pass the first stage
  (init!))

