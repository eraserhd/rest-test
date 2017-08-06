(ns rest-test.core
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [ring.middleware.json]))

(s/def ::last-name string?)
(s/def ::first-name (s/and string? not-empty))
(s/def ::gender (s/and string? not-empty))
(s/def ::record (s/keys :req [::first-name
                              ::gender
                              ::favorite-color
                              ::birthdate]))
(s/def ::parsed-body (s/coll-of ::record :kind set?))

(defn not-found
  [request]
  {:status 404
   :body "Not found!"})

(defn- parse-body
  [body]
  (into #{}
        (comp
          (map #(string/split % #"[,| ]"))
          (map (fn [[last-name first-name gender favorite-color birthdate]]
                 {::last-name last-name
                  ::first-name first-name
                  ::gender gender
                  ::favorite-color favorite-color
                  ::birthdate birthdate})))
        (string/split body #"\n")))

(defn- post-records
  [handler]
  (fn post-records* [{:keys [request-method uri state body] :as request}]
    (if-not (= [request-method uri] [:post "/records"])
      (handler request)
      (let [parsed-body (parse-body (slurp body))]
        (if (s/valid? ::parsed-body parsed-body)
          {:status 200
           :state (into state parsed-body)
           :body {:status "ok"}}
          {:status 400
           :body {:status "error"
                  :error (s/explain-str ::parsed-body parsed-body)}})))))

(def pure-handler
  (-> not-found
    post-records
    ring.middleware.json/wrap-json-response
    (ring.middleware.json/wrap-json-body :keywords? true)))

(defn wrap-state
  "Ring middleware to track application state.

  Wrapped middleware is supplied with the current state in the `:state` key
  in the request.  If a `:state` key is returned in the wrapped middleware's
  response, the new state is updated.  This is done in a Clojure STM
  transaction to prevent races."
  [inner-handler initial-value]
  (let [state-atom (atom initial-value)]
    (fn [request]
      (dosync
        (let [result (inner-handler (assoc request :state @state-atom))]
          (when (contains? result :state)
            (reset! state-atom (:state result)))
          result)))))

(def handler (wrap-state pure-handler []))
