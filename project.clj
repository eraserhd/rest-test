(defproject rest-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [ring/ring-core "1.6.2"]
                 [ring/ring-jetty-adapter "1.6.2"]]
  :profiles {:dev {:dependencies [[midje "1.9.0-alpha6"]]
                   :plugins [[lein-midje "3.2.1"]
                             [lein-ring "0.12.0"]]}}
  :ring {:handler rest-test.core/handler}
  :repl-options {:init-ns rest-test.repl})
