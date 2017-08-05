(ns rest-test.core
  (:require [ring.middleware.json]))

(defn not-found
  [request]
  {:status 404
   :body "Not found!"})

(def pure-handler
  (-> not-found
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
