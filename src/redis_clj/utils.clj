(ns redis-clj.utils 
  (:require
   [clojure.string :as str]))

(defn expiry->ttl
  "rename."
  [unit-type unit-value]
  (case (str/lower-case unit-type)
    "px" (Long/parseLong unit-value)
    "ex" (* 1000 (Long/parseLong unit-value))
    nil nil))
