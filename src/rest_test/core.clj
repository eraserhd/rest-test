(ns rest-test.core
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [rest-test.state :as state]
            [ring.middleware.json])
  (:import [java.text SimpleDateFormat]))

(def input-files
  ["comma-delimited.txt"
   "pipe-delimited.txt"
   "space-delimited.txt"])

(s/def ::last-name string?)
(s/def ::first-name (s/and string? not-empty))
(s/def ::gender (s/and string? not-empty))
(s/def ::favorite-color (s/and string? not-empty))

(defn- date-valid?
  [s]
  (let [date-format (doto (SimpleDateFormat. "yyyy-MM-dd")
                      (.setLenient false))]
    (try
      (.parse date-format s)
      true
      (catch Throwable t
        false))))

;; We supply our own generator for ::birthdate because the default one
;; would generate random unicode strings until it finds a valid birthdate.
;; How many, on average, would need to be generated before finding a valid
;; birth date is left as an exercise to the reader.
;;
;; (We still need gen/such-that to prevent things like February 31st.)
(s/def ::birthdate
  (s/spec (s/and string? date-valid?)
          :gen (fn make-birthdate-generator []
                 (gen/such-that
                   date-valid?
                   (gen/fmap
                     (fn [[year month day]]
                       (format "%04d-%02d-%02d" year month day))
                     (gen/tuple
                       (gen/choose 1890 2017)
                       (gen/choose 1 12)
                       (gen/choose 1 31)))))))


(s/def ::record (s/keys :req [::first-name
                              ::gender
                              ::favorite-color
                              ::birthdate]
                        :opt [::last-name]))
(s/def ::parsed-body (s/coll-of ::record :kind set?))

(defn- not-found-handler
  [request]
  {:status 404
   :body "Not found!"})

(defn- parse-file
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

(defn- post-handler
  [handler]
  (fn post-handler* [{:keys [request-method uri state body] :as request}]
    (if-not (= [request-method uri] [:post "/records"])
      (handler request)
      (let [parsed-body (parse-file (slurp body))]
        (if (s/valid? ::parsed-body parsed-body)
          {:status 200
           :state (into state parsed-body)
           :body {:status "ok"}}
          {:status 400
           :body {:status "error"
                  :error (s/explain-str ::parsed-body parsed-body)}})))))

(defn- format-date
  [internal-format]
  (let [[year month day] (string/split internal-format #"-")]
    (format "%d/%d/%s" (Long/parseLong month) (Long/parseLong day) year)))

(defn- uncapitalize
  [s]
  (str (.toLowerCase (subs s 0 1)) (subs s 1)))

(defn- wrap-json-preferred-keys
  "Replace keywords in data with strings preferred in JSON.

  e.g. :first-name becomes \"firstName\""
  [handler]
  (fn wrap-json-preferred-keys* [request]
    (let [response (handler request)]
      (cond-> response
        (coll? (:body response))
        (update :body (fn [body]
                        (walk/postwalk
                          (fn [entity]
                            (if (keyword? entity)
                              (->> (string/split (name entity) #"-")
                                (map string/capitalize)
                                string/join
                                uncapitalize)
                              entity))
                          body)))))))

(defn- get-handler
  [handler kind sort-key-fn descending?]
  (fn get-records* [{:keys [request-method uri state] :as request}]
    (if-not (= [request-method uri] [:get (str "/records/" kind)])
      (handler request)
      {:status 200
       :body {:records (cond->> state
                         true        (sort-by sort-key-fn)
                         descending? reverse  ; Didn't actually mean to be cond-descending here,
                                              ; but I couldn't help it.
                         true        (map #(update % ::birthdate format-date)))}})))

(def pure-handler
  (-> not-found-handler
    post-handler
    (get-handler "gender" (juxt ::gender ::last-name) false)
    (get-handler "birthdate" ::birthdate false)
    (get-handler "name" ::last-name true)
    wrap-json-preferred-keys
    ring.middleware.json/wrap-json-response))

;; Everything below this line isn't (mechanically) tested

(defn- load-resources
  []
  (let [state (reduce
                (fn [state filename]
                  (into state (parse-file (slurp (io/resource filename)))))
                #{}
                input-files)]
    (when-not (s/valid? ::parsed-body state)
      (s/explain ::parsed-body state))
    state))

(def handler (state/wrap-state pure-handler (load-resources)))
