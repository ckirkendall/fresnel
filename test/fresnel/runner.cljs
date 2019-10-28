(ns fresnel.runner
  (:require  [cljs.test :as t :include-macros true :refer [report]]
             [doo.runner :include-macros true :refer [doo-all-tests]]
             [fresnel.lenses-test]))

(doo-all-tests #"fresnel.*test")
