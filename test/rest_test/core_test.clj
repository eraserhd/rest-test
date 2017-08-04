(ns rest-test.core-test
  (:require [midje.sweet :refer :all]
            [rest-test.core :as core]))

(fact "math still works"
  (+ 2 2) => 4)
