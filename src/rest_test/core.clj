(ns rest-test.core)


(defn wrap-state
  "Ring middleware to track application state.

  Wrapped middleware is supplied with the current state in the `:state` key
  in the request."
  [inner-handler initial-value]
  (let [state-atom (atom initial-value)]
    (fn [request]
      (inner-handler (assoc request :state @state-atom)))))

(defn handler
  [request])
