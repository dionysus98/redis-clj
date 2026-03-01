(ns redis-clj.core
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:import [java.io BufferedReader BufferedWriter]
           [java.net ServerSocket Socket])
  (:gen-class))

(def ^:const PORT 6379)

(defn handle-msg!
  [& args]
  (log/info :args args)
  "+PONG\r\n")

(defn handle-conn!
  [^Socket socket ^clojure.lang.IFn handler]
  (try
    (let [^BufferedReader reader (io/reader socket)
          ^BufferedWriter writer (io/writer socket)]
      (log/info :msg "Handling Message")
      (loop []
        (when-let [msg (.readLine ^String reader)]
          (.write writer (handler msg))
          (.flush writer)
          (recur))))
    (catch Exception e
      (log/error :error e))))

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
        (future (handle-conn! client-sock handler))))))

(defn init! [& _]
  (log/info :msg "serving on port: " PORT)
  (serve! PORT handle-msg!))

(defn -main
  "I don't do a whole lot ... yet."
  [& _]
  ;; You can use print statements as follows for debugging, they'll be visible when running tests.
  (log/info "Logs from your program will appear here!")
  ;;[x] Uncomment the code below to pass the first stage
  (init!))

(comment
  (future (init!))

  (.close @!server-socket)
  (.isClosed @!server-socket)

  (require '[aleph.tcp :as tcp]
           '[manifold.stream :as s]
           '[byte-streams :as bs])


  (def !tcp-conn (atom nil))
  (.close @!tcp-conn)

  (try
    (let [conn (reset! !tcp-conn @(tcp/client {:host "localhost" :port PORT}))]

      (s/consume
       (fn [msg]
         (log/info :consumed (bs/to-string msg)))
       conn)

      (future
        (doseq [msg ["PING\nPING\n"]]
          #_(log/info :conn conn)
          @(s/put! conn msg))))
    (catch Exception e e))

  :rcf)
