(ns rest-test.json
  (:require [clojure.walk :as walk]))

(defn wrap-json-preferred-keys
  "Replace keywords in data with strings preferred in JSON.

  e.g. :some.namespace/firstName becomes \"firstName\""
  [handler]
  (fn wrap-json-preferred-keys* [request]
    (let [response (handler request)]
      (update response :body (fn [body]
                               (walk/postwalk
                                 (fn [entity]
                                   (cond-> entity
                                     (keyword? entity)
                                     name))
                                 body))))))
