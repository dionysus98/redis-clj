(ns redis-clj.resp.protocol
  "REF: https://redis.io/docs/latest/develop/reference/protocol-spec/#resp-protocol-description")

(def ^:const CRLF "\r\n")

(def ^:const symbol->type
  {\+ :simple-string
   \- :error
   \: :integer
   \# :boolean
   \$ :bulk-string
   \* :array})

(def ^:const type->symbol
  (into {} (map (fn [[k v]] [v k])) symbol->type))

