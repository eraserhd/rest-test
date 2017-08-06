(ns rest-test.repl
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [midje.config]))

;; Convince Midje we are started from the REPL even though we're requiring
;; it from a compiled file -- Midje won't load the online docs otherwise.
(alter-var-root #'midje.config/started-in-repl? (constantly true))
(use 'midje.repl)
