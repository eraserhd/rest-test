(ns rest-test.core)

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

(defn handler
  [request]
  {:status 200
   :body "Hello, world!"})
