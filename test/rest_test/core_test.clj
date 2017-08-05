(ns rest-test.core-test
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [rest-test.core :as core]))

(defn- handle
  [& [{:keys [request initial-state handler-result]
       :or {requeset {},
            initial-state [],
            handler-result {}}}]]
  (let [passed-request (atom nil)
        handler (core/wrap-state
                  (fn [request]
                    (reset! passed-request request)
                    handler-result)
                  initial-state)
        result (handler {})]
    {:result result
     :passed-request @passed-request}))

(facts "about the `wrap-state` ring middleware"
  (fact "`wrap-state` passes initial state to the wrapped middleware"
    (:passed-request (handle {:initial-state 42})) => (contains {:state 42}))
  (fact "`wrap-state` returns the wrapped middleware's response"
    (:result (handle {:handler-result {:foo "hi, mom!"}})) => {:foo "hi, mom!"}))
