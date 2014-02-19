(ns segments)

(defprotocol Segment
  (-fetch [seg value])
  (-putback [seg value subvalue]))

(defn fetch [value seg]
  (-fetch seg value))

(defn putback [value seg subvalue]
  (-putback seg value subvalue))
  

(extend-type #+clj clojure.lang.Keyword #+cljs cljs.core.Keyword
    Segment
    (-fetch [seg value] (get value seg))
    (-putback [seg value subval] (assoc value seg subval)))

(extend-type #+clj clojure.lang.Symbol #+cljs cljs.core.Symbol
    Segment
    (-fetch [seg value] (get value seg))
    (-putback [seg value subval] (assoc value seg subval)))

(extend-type #+clj String #+cljs string
    Segment
    (-fetch [seg value] (get value seg))
    (-putback [seg value subval] (assoc value seg subval)))

(extend-type #+clj Number #+cljs number
  Segment
  (-fetch [seg value] (nth value seg))
  (-putback [seg value subval] (assoc value seg subval)))


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
                          (assoc m k (case k
                                       :fetch `(fn [_# ~value-arg] ~v)
                                       :putback `(fn [_# ~value-arg ~subvalue-arg] ~v))))
                        {}
                        (partition 2 methods))
        f `(fn [~@plain-args ~@(when plain-rest-arg `[& ~plain-rest-arg])]
             (let [~@(interleave args plain-args)
                   ~@(when plain-rest-arg [rest-arg plain-rest-arg])]
               (segments/create-segment ~(:fetch methods) ~(:putback methods) )))]
    `(def ~name ~(if auto-segment (list f) f))))


(defn fetch-in [obj path]
  (reduce fetch obj path))


(defn putback-in [obj [seg & path] value]
  (when (and obj seg)
    (if (empty? path)
      (putback obj seg value)
      (putback obj seg (putback-in (fetch obj seg) path value)))))
