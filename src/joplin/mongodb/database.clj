(ns joplin.mongodb.database
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [joplin.core :refer :all]
            [ragtime.protocols :refer [DataStore]])
  (:import com.mongodb.DuplicateKeyException))


;; =============================================================

(defn ensure-migration-schema
  "Ensures the migration schema is loaded. Use `migrations` collection by default"
  [conn collection]
  (mc/create-index conn collection (array-map :id 1) {:unique true}))

(defn get-connection [uri]
  (mg/connect-via-uri uri))

(defn with-connection [uri f]
  (when-let [{:keys [db conn]} (get-connection uri)]
    (try
      (f db)
      (finally (mg/disconnect conn)))))

;; ============================================================================
;; Ragtime interface

(defrecord MongodbDatabase [uri collection]
  DataStore
  (add-migration-id [_ id]
    (with-connection uri
      (fn [db]
        (ensure-migration-schema db collection)
        (try
          (mc/insert db collection {:id id})
          (catch DuplicateKeyException e)))))

  (remove-migration-id [_ id]
    (with-connection uri
      (fn [db]
        (ensure-migration-schema db collection)
        (mc/remove db collection {:id id}))))

  (applied-migration-ids [_]
    (with-connection uri
      (fn [db]
        (ensure-migration-schema db collection)
        (->> (mc/find-maps db collection)
             (map :id)
             sort
             (into []))))))

(defn ->MongodbDatabase [{:keys [db]}]
  (let [{:keys [uri collection]} db
        collection* (or collection "migrations")]
    (MongodbDatabase. uri collection*)))

;; ============================================================================
;; Joplin interface

(defmethod migrate-db :mongodb [target & args]
  (apply do-migrate (get-migrations (:migrator target))
         (->MongodbDatabase target) args))

(defmethod rollback-db :mongodb [target amount-or-id & args]
  (apply do-rollback (get-migrations (:migrator target))
         (->MongodbDatabase target) amount-or-id args))

(defmethod seed-db :mongodb [target & args]
  (apply do-seed-fn (get-migrations (:migrator target))
         (->MongodbDatabase target) target args))

(defmethod pending-migrations :mongodb [target & args]
  (do-pending-migrations (->MongodbDatabase target)
                         (get-migrations (:migrator target))))

(defmethod create-migration :mongodb [target id & args]
  (do-create-migration target id "joplin.mongodb.database"))

;; ============================================================================
;; Migration interface

(defmacro as-db->
  "Like as-> but threads database connection"
  [expr name & forms]
  `(let [uri# (:uri ~expr)]
     (with-connection uri#
       (fn [~'db]
         (as-> ~name ~'db ~@forms)))))
