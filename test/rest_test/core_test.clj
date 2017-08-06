(ns rest-test.core-test
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [rest-test.core :as core]))

(facts "about the REST service"
  (fact "random URLs respond with 404"
    (:status (core/pure-handler (mock/request :get "/somewhere/random"))) => 404)
  (facts "about POST /records"
    (fact "posts to /records accept CSV"
      (let [csv-post (-> (mock/request :post "/records")
                       (mock/header "Content-Type" "text/csv")
                       (mock/body (str "Fabetes,Joe,male,blue,1997-02-12\n"
                                       "Smith,Jane,female,green,1973-05-06\n")))]
        (:status (core/pure-handler csv-post)) => 200))))


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
    (:passed-request (first (results [{:initial-state 42}]))) => (contains {:state 42}))
  (fact "`wrap-state` returns the wrapped middleware's response"
    (:result (first (results [{:handler-result {:foo "hi, mom!"}}]))) => {:foo "hi, mom!"})
  (fact "`wrap-state` updates the state if returned by the wrapped middleware"
    (:passed-request (last (results [{:initial-state 42,
                                      :handler-result {:state 79}}
                                     {}]))) => (contains {:state 79})))
