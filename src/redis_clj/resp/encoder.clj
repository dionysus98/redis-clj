(ns redis-clj.resp.encoder
  (:require
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [redis-clj.resp.protocol :as rp]))

(defn with-crlf [prefix v]
  (str prefix v rp/CRLF))

(defmulti encode :type)

(defmethod encode :simple-string [{:keys [type value]}]
  (log/info :msg "encoding RESP" type)
  (with-crlf (rp/type->symbol type) value))

(defmethod encode :error [{:keys [type value]}]
  (log/info :msg "encoding RESP" type)
  (with-crlf (rp/type->symbol type) value))

(defmethod encode :integer [{:keys [type value]}]
  (with-crlf (rp/type->symbol type) value))

(defmethod encode :boolean [{:keys [type value]}]
  (log/info :msg "encoding RESP" type)
  (-> (rp/type->symbol type)
      (with-crlf (case value
                    true "t"
                    false "f"))))

(defmethod encode :bulk-string [{:keys [type value]}]
  (log/info :msg "encoding RESP" type)
  (if (nil? value)
    (-> (rp/type->symbol type)
        (with-crlf -1))
    (-> (rp/type->symbol type)
        (with-crlf (count (.getBytes value "UTF-8")))
        (with-crlf value))))

(defmethod encode :array [{:keys [type value]}]
  (log/info :msg "encoding RESP" type)
  (-> (rp/type->symbol type)
      (with-crlf (count value))
      (str (str/join (mapv encode value)))))


;; === HELPERS ===
(defn simple-string [value]
  (encode {:type :simple-string :value value}))

(defn error [value]
  (encode {:type :error :value value}))

(defn integer [value]
  (encode {:type :integer :value value}))

(defn boolean [value]
  (encode {:type :boolean :value value}))

(defn bulk-string [value]
  (encode {:type :bulk-string :value value}))

(defn array [value]
  (encode {:type :array :value value}))
