(ns rest-test.core-test
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [rest-test.core :as core]))

(def ^:private content-types
  {"," "text/csv"
   "|" "text/plain"
   " " "text/plain"})

(defn- post
  [delimiter & [{:keys [initial-state fields]
                 :or {initial-state #{}
                      fields ["Fabetes" "Joe" "male" "blue" "1997-02-12"]}}]]
  (-> (mock/request :post "/records")
    (mock/header "Content-Type" (get content-types delimiter))
    (mock/body (str
                 (string/join delimiter fields) "\n"
                 (string/join delimiter ["Smith" "Jane" "female" "green" "1973-05-06"]) "\n"))
    (assoc :state initial-state)
    core/pure-handler))

(facts "about the REST service"
  (fact "random URLs respond with 404"
    (:status (core/pure-handler (mock/request :get "/somewhere/random"))) => 404)
  (facts "about posts to /records"
    (fact "posts to /records accept comma-separated values"
      (:status (post ",")) => 200
      (:state (post ",")) => #{{::core/last-name "Fabetes",
                                ::core/first-name "Joe",
                                ::core/gender "male",
                                ::core/favorite-color "blue",
                                ::core/birthdate "1997-02-12"}
                               {::core/last-name "Smith",
                                ::core/first-name "Jane",
                                ::core/gender "female"
                                ::core/favorite-color "green"
                                ::core/birthdate "1973-05-06"}})
    (fact "posts to /records accept pipe-delimited records"
      (:status (post "|")) => 200
      (:state (post "|")) => #{{::core/last-name "Fabetes",
                                ::core/first-name "Joe",
                                ::core/gender "male",
                                ::core/favorite-color "blue",
                                ::core/birthdate "1997-02-12"}
                               {::core/last-name "Smith",
                                ::core/first-name "Jane",
                                ::core/gender "female"
                                ::core/favorite-color "green"
                                ::core/birthdate "1973-05-06"}})
    (fact "posts to /records accept space-delimited records"
      (:status (post " ")) => 200
      (:state (post " ")) => #{{::core/last-name "Fabetes",
                                ::core/first-name "Joe",
                                ::core/gender "male",
                                ::core/favorite-color "blue",
                                ::core/birthdate "1997-02-12"}
                               {::core/last-name "Smith",
                                ::core/first-name "Jane",
                                ::core/gender "female"
                                ::core/favorite-color "green"
                                ::core/birthdate "1973-05-06"}})
    (fact "posts to /records preserve existing records"
      (let [initial-state #{{::core/last-name "Begone"
                             ::core/first-name "Bob"
                             ::core/gender "male"
                             ::core/favorite-color "mauve"
                             ::core/birthdate "1981-07-01"}}]
        (:state (post "," {:initial-state initial-state})) => #{{::core/last-name "Fabetes",
                                                                 ::core/first-name "Joe",
                                                                 ::core/gender "male",
                                                                 ::core/favorite-color "blue",
                                                                 ::core/birthdate "1997-02-12"}
                                                                {::core/last-name "Smith",
                                                                 ::core/first-name "Jane",
                                                                 ::core/gender "female"
                                                                 ::core/favorite-color "green"
                                                                 ::core/birthdate "1973-05-06"}
                                                                {::core/last-name "Begone",
                                                                 ::core/first-name "Bob",
                                                                 ::core/gender "male",
                                                                 ::core/favorite-color "mauve"
                                                                 ::core/birthdate "1981-07-01"}}))
    (fact "successful posts to /records respond with a JSON success message"
      (:headers (post ",")) => (contains {"Content-Type" #"application/json"})
      (json/parse-string (:body (post ","))) => {"status" "ok"})
    (facts "posts to /records validate input fields"
      (fact "first name must not be empty"
        (:status (post "," {:fields ["Fabetes" "" "male" "blue" "1997-02-12"]})) => 400)
      (fact "last name can be empty"
        (:status (post "," {:fields ["" "Joe" "male" "blue" "1997-02-12"]})) => 200)
      (fact "gender must not be empty"
        (:status (post "," {:fields ["Fabetes" "Joe" "" "blue" "1997-02-12"]})) => 400)
      (fact "favorite color must not be empty"
        (:status (post "," {:fields ["Fabetes" "Joe" "male" "" "1997-02-12"]})) => 400)
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
