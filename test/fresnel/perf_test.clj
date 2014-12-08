(ns fresnel.perf-test
  (:require [fresnel.core :refer :all]))

(def state {:a [{:b [:c]}]})

(defn run-f-test [state path cnt]
  (time
   (dotimes [_ cnt]
     (fetch state path))))

(defn run-test [state path cnt]
  (time
   (dotimes [_ cnt]
     (get-in state path))))


