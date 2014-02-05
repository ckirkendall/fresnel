(ns segments)


(defprotocol Segment
  (-fetch [seg value])
  (-putback [seg value subvalue]))


(defn fetch [value seg]
  (-fetch seg value))


(defn putback [value seg subvalue]
  (-putback seg value subvalue))


(def base-extend-list [clojure.lang.Keyword 
                       #+clj clojure.lang.Symbol 
                       #+cljs string
                       #+clj String])  


(doseq [t base-extend-list]
  (extend t
    Segment {:-fetch #(get %2 %1)
             :-putback #(assoc %2 %1 %3)}))


(extend Number
  Segment {:-fetch #(nth %2 %1)
           :-putback #(assoc %2 %1 %3)})



(defn create-segment [fetch putback]
  (reify Segment
    (-fetch [seg value] (fetch seg value))
    (-putback [seg value subvalue] (putback seg value subvalue))))



(defmacro defsegment [name doc? initial-args & methods]
  (let [[name initial-args methods] (if (string? doc?)
                                      [(vary-meta name assoc :doc doc?) initial-args methods]
                                      [name doc? (cons initial-args methods)])
        [value-arg subvalue-arg & args] initial-args
        auto-segment (empty? args)
        [args [_ & rest-arg]] (split-with #(not= (clojure.core/name %) "&") args)
        plain-args (take (count args) (repeatedly gensym))
        plain-rest-arg (when rest-arg (gensym))
        fetch-expr (some (fn [[name expr]] (case name :fetch expr nil)) (partition 2 methods))
        methods (reduce (fn [m [k v]]
                          (assoc m k  (case k
                                        :fetch `(fn [_# ~value-arg] ~expr)
                                        :putback `(fn [_# ~value-arg ~subvalue-arg] ~expr))))
                        (partition 2 methods))
        f `(fn [~@plain-args ~@(when plain-rest-arg `[& ~plain-rest-arg])]
             (let [~@(interleave args plain-args)
                   ~@(when plain-rest-arg [rest-arg plain-rest-arg])]
               (create-segment ~(:fetch methods) ~(:putback methods) )))]
    `(def ~name ~(if auto-segment (list f) f))))



(defn fetch-in [obj path]
  (reduce fetch obj path))


(defn putback-in [obj value [seg & path]]
  (when (and obj seg)
    (if (empty? path)
      (putback obj seg value)
      (putback obj seg (putback-in (fetch obj seg) value path)))))
