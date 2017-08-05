(ns rest-test.core-test
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [rest-test.core :as core]))

(defn- results
  [requests]
  (let [passed-request-atom (atom nil)
        handler-result-atom (atom nil)
        initial-state (or (:initial-state (first requests)) [])
        handler (core/wrap-state
                  (fn [request]
                    (reset! passed-request-atom request)
                    @handler-result-atom)
                  initial-state)]
    (doall
      (for [{:keys [request handler-result]
             :or {request {},
                  handler-result {}}} requests]
        (do
          (reset! handler-result-atom handler-result)
          (let [result (handler request)]
            {:result result
             :passed-request @passed-request-atom}))))))

(facts "about the `wrap-state` ring middleware"
  (fact "`wrap-state` passes initial state to the wrapped middleware"
    (->> (results [{:initial-state 42}])
      (map :passed-request)
      first) => (contains {:state 42}))
  (fact "`wrap-state` returns the wrapped middleware's response"
    (->> (results [{:handler-result {:foo "hi, mom!"}}])
      (map :result)
      first) => {:foo "hi, mom!"}))
