(ns redis-clj.core
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:import [java.io BufferedReader BufferedWriter]
           [java.net ServerSocket Socket])
  (:gen-class))


;; ===========
;; note: adding all this type hints so I'd understand what happens BTS. 
;;       cause i'm dumb.
;; ===========


(defn receive-message
  "Read a line of textual data from the given socket"
  ^String
  [^Socket socket]
  (let [^BufferedReader reader (io/reader socket)]
    (.readLine ^String reader)))

(defn send-message
  "Send the given string message out over the given socket"
  [^Socket socket ^String msg]
  (log/info :msg {:socket socket :msg msg})
  (let [^BufferedWriter writer (io/writer socket)]
    (.write writer msg)
    (.flush writer)))

(defonce !server-socket (atom nil))

(defn serve [port handler]
  (with-open [^ServerSocket server-sock (reset! !server-socket (ServerSocket. port))]
    ;; Since the tester restarts your program quite often, setting SO_REUSEADDR
    ;; ensures that we don't run into 'Address already in use' errors
    (.setReuseAddress server-sock true)

    (with-open [^Socket client-sock (.accept ^Socket server-sock)]
      (let [msg-in (receive-message client-sock)
            msg-out (handler msg-in)]
        (send-message client-sock msg-out)))))

(defn handler
  [& args]
  (log/info :args args)
  "+PONG\r\n")

(defn init! [& _]
  (log/info :msg "serving on port: " 6379)
  (serve 6379 handler))

(defn -main
  "I don't do a whole lot ... yet."
  [& _]
  ;; You can use print statements as follows for debugging, they'll be visible when running tests.
  (log/info "Logs from your program will appear here!")
  ;;[x] Uncomment the code below to pass the first stage
  (init!))

(comment
  (future (init!))

  :rcf)