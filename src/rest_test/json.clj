(ns rest-test.json
  (:require [clojure.string :as string]
            [clojure.walk :as walk]))

(defn- uncapitalize
  [s]
  (str (.toLowerCase (subs s 0 1)) (subs s 1)))

(defn wrap-json-preferred-keys
  "Replace keywords in data with strings preferred in JSON.

  e.g. :first-name becomes \"firstName\""
  [handler]
  (fn wrap-json-preferred-keys* [request]
    (let [response (handler request)]
      (cond-> response
        (coll? (:body response))
        (update :body (fn [body]
                        (walk/postwalk
                          (fn [entity]
                            (if (keyword? entity)
                              (->> (string/split (name entity) #"-")
                                (map string/capitalize)
                                string/join
                                uncapitalize)
                              entity))
                          body)))))))

