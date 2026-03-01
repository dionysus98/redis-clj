(ns redis-clj.resp.protocol)

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

