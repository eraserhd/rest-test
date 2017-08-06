(ns rest-test.core-test
  (:require [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as string]
            [clojure.test.check]
            [clojure.test.check.properties :as prop]
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

(defn holds?
  "Does a property hold?

  A custom midje checker used so that we can see the whole failed test.check
  output when it fails."
  [result]
  (= true (:result result)))

(defmacro property
  "Make a test.check property into a Midje fact."
  ([descr prop]
   `(property ~descr 25 ~prop))
  ([descr trials prop]
   `(fact ~descr
      (clojure.test.check/quick-check ~trials ~prop) => holds?)))

(defn- records
  [endpoint-kind state]
  (let [result (-> (mock/request :get (str "/records/" endpoint-kind))
                 (assoc :state state)
                 core/pure-handler)
        json-result (json/parse-string (:body result) keyword)]
    (:records json-result)))

(defn- in-ascending-order?
  [coll]
  (every? (fn [[a b]] (<= (compare a b) 0)) (partition 2 1 coll)))

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
      (fact "birthdate must be a valid date"
        (:status (post "," {:fields ["Fabetes" "Joe" "male" "blue" ""]})) => 400
        (:status (post "," {:fields ["Fabetes" "Joe" "male" "blue" "garbage"]})) => 400
        (:status (post "," {:fields ["Fabetes" "Joe" "male" "blue" "1992-02-30"]})) => 400
        (:status (post "," {:fields ["Fabetes" "Joe" "male" "blue" "1992-03-31"]})) => 200)))
  (property "all record retrieval endpoints return all records"
    (prop/for-all [state (s/gen ::core/parsed-body)
                   endpoint-type (gen/elements ["gender" "birthdate" "name"])]
      (= (count state) (count (records endpoint-type state)))))
  (property "all record retrieval endpoints return dates in M/D/YYYY format"
    (prop/for-all [state (s/gen ::core/parsed-body)
                   endpoint-type (gen/elements ["gender" "birthdate" "name"])]
        (every? #(re-matches #"[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}" %)
                (map ::core/birthdate (records endpoint-type state)))))
  (property "/records/gender returns records sorted by gender, then by last name" 50
    (prop/for-all [state (s/gen ::core/parsed-body)]
      (in-ascending-order? (->> (records "gender" state)
                             (map (juxt ::core/gender ::core/last-name))))))
  (property "/records/birthdate returns records sorted by birthdate" 50
    (prop/for-all [state (s/gen ::core/parsed-body)]
      (in-ascending-order? (->> (records "birthdate" state)
                             (map ::core/birthdate)
                             (map #(.parse (java.text.SimpleDateFormat. "M/d/yyyy") %))))))
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
