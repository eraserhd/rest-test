(ns rest-test.core-test
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [rest-test.core :as core]))

(facts "about the `wrap-state` ring middleware"
  (fact "`wrap-state` passes initial state to the wrapped middleware"
    (let [passed-request (atom nil)
          handler (core/wrap-state
                    (fn [request]
                      (reset! passed-request request)
                      {})
                    42)
          result (handler {})]
      @passed-request => (contains {:state 42}))))
