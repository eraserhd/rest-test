(ns rest-test.core-test
  (:require [clojure.string :as string]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [rest-test.core :as core]))

(def ^:private content-types
  {"," "text/csv"
   "|" "text/plain"
   " " "text/plain"})

(defn- post
  [delimiter]
  (-> (mock/request :post "/records")
    (mock/header "Content-Type" (get content-types delimiter))
    (mock/body (str
                 (string/join delimiter ["Fabetes" "Joe" "male" "blue" "1997-02-12"]) "\n"
                 (string/join delimiter ["Smith" "Jane" "female" "green" "1973-05-06"]) "\n"))
    (assoc :state [])
    core/pure-handler))

(facts "about the REST service"
  (fact "random URLs respond with 404"
    (:status (core/pure-handler (mock/request :get "/somewhere/random"))) => 404)
  (facts "about posts to /records"
    (fact "posts to /records accept comma-separated values"
      (:status (post ",")) => 200
      (:state (post ",")) => #{{:last-name "Fabetes",
                                :first-name "Joe",
                                :gender "male",
                                :favorite-color "blue",
                                :birthdate "1997-02-12"}
                               {:last-name "Smith",
                                :first-name "Jane",
                                :gender "female"
                                :favorite-color "green"
                                :birthdate "1973-05-06"}})
    (fact "posts to /records accept pipe-delimited records"
      (:status (post "|")) => 200
      (:state (post "|")) => #{{:last-name "Fabetes",
                                :first-name "Joe",
                                :gender "male",
                                :favorite-color "blue",
                                :birthdate "1997-02-12"}
                               {:last-name "Smith",
                                :first-name "Jane",
                                :gender "female"
                                :favorite-color "green"
                                :birthdate "1973-05-06"}})
    (pending-fact "posts to /records accept space-delimited records")
    (pending-fact "posts to /records preserve existing records")
    (facts "posts to /records validate input fields"
      (pending-fact "first name must not be empty")
      (pending-fact "last name can be empty")
      (pending-fact "gender must not be empty")
      (pending-fact "favorite color must not be empty")
      (pending-fact "birthdate must be a valid date")))
  (pending-fact "all record retrieval endpoints return dates in M/D/YYYY format")
  (pending-fact "/records/gender returns records sorted by gender")
  (pending-fact "/records/birthdate returns records sorted by birthdate")
  (pending-fact "/records/name returns records sorted by name"))


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
