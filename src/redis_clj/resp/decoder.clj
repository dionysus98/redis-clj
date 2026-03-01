(ns
 ^{:doc "[RESP protocol](https://redis.io/docs/latest/develop/reference/protocol-spec/)"}
 redis-clj.resp.decoder
  (:require
   [clojure.tools.logging :as log]
   [redis-clj.resp.protocol :as rp])
  (:import
   [java.io BufferedReader]))


;; - In RESP, the first byte of data determines its type.
;; - Clients send commands to a Redis server as an array of bulk strings. 
;; - The server replies with a RESP type. 
;;   - (The reply's type is determined by the command's implementation.)

(def ^:const newline-byte (byte \newline))
(def ^:const return-byte (byte \return))
(def ^:const space-byte (byte \space))

(defn read-until-crlf
  "TODO: fix this. handle \r\n properly. read the docs.
   Maybe you don't have to do all these checks."
  [^BufferedReader reader]
  (loop [string    ""
         prev-byte nil]
    (if-let [byte (.read ^String reader)]
      (cond
        (and (= prev-byte return-byte) (= byte newline-byte))
        string

        (= byte return-byte)
        (recur string byte)

        (= prev-byte return-byte)
        (recur (str string (char prev-byte) (char byte)) byte)

        :else
        (recur (str string (char byte)) byte))
      string)))

(defn read-n
  "Returns string. 
   - read char for `n` number of times."
  [^BufferedReader reader ^Number n]
  (loop [string ""
         n      n]
    (if (zero? n)
      string
      (recur (str string (char (.read ^String reader)))
             (dec n)))))

(defmulti decode*
  (fn [prefix ^BufferedReader _]
    prefix))

(defn decode
  "Main entry for RESP decoder.
   Internally calls decode* multimethod to parse RESP.
   This function exists to keep the dispatcher fn pure."
  [^BufferedReader reader]
  (try
    (when-let [prefix (char (.read reader))]
      (decode* prefix reader))
    (catch Exception _)))

;; Simple string
(defmethod decode* \+ [prefix ^BufferedReader reader]
  (log/info :msg "Parsing RESP :simple-string")
  {:type  (rp/symbol->type prefix)
   :value (read-until-crlf reader)})

;; Error
(defmethod decode* \- [prefix ^BufferedReader reader]
  (log/info :msg "Parsing RESP :error")
  {:type  (rp/symbol->type prefix)
   :value (read-until-crlf reader)})

;; Integer
(defmethod decode* \: [prefix ^BufferedReader reader]
  (log/info :msg "Parsing RESP :integer")
  {:type  (rp/symbol->type prefix)
   :value (read-until-crlf reader)})

;; Boolean
(defmethod decode* \# [prefix ^BufferedReader reader]
  (log/info :msg "Parsing RESP :boolean")
  {:type  (rp/symbol->type prefix)
   :value (case (read-until-crlf reader)
            "t" true
            "f" false)})

;; Bulk string
(defmethod decode* \$ [prefix ^BufferedReader reader]
  (log/info :msg "Parsing RESP :bulk-string")
  (let [str-length (Long/parseLong (read-until-crlf reader))
        string     (read-n reader str-length)]
    (assert (= (read-until-crlf reader) "") "invalid RESP")
    {:type  (rp/symbol->type prefix)
     :value string}))

;; Array
(defmethod decode* \* [prefix ^BufferedReader reader]
  (log/info :msg "Parsing RESP :array")
  (let [arr-length (Long/parseLong (read-until-crlf reader))
        arr        (loop [arr []
                          n   arr-length]
                     (if (zero? n)
                       arr
                       (recur (conj arr (decode reader))
                              (dec n))))]
    {:type  (rp/symbol->type prefix)
     :value arr}))

