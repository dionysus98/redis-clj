(ns dev.user
  (:require
   [clojure.tools.logging :as log]
   [redis-clj.core :as core]
   [redis-clj.const :as const]))

(comment
  (future (core/init! {:port const/PORT}))

  (.close @core/!server-socket)
  (.isClosed @core/!server-socket)


  (require '[aleph.tcp :as tcp]
           '[manifold.stream :as s]
           '[byte-streams :as bs])


  (def !tcp-conn (atom nil))
  (.close @!tcp-conn)

  (try
    (let [conn (reset! !tcp-conn @(tcp/client {:host "localhost" :port core/PORT}))]

      (s/consume
       (fn [msg]
         (log/info :consumed (bs/to-string msg)))
       conn)

      (future
        (doseq [msg ["+PING\r\n+PING\r\n"
                     "*2\r\n$4\r\nECHO\r\n$3\r\nhey\r\n"]]
          #_(log/info :conn conn)
          @(s/put! conn msg))))
    (catch Exception e e))

  :rcf)
