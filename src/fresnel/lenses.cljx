(ns fresnel.lenses)

(declare fetch-in putback-in)

(defprotocol Lens
  (-fetch [seg value])
  (-putback [seg value subvalue]))



(defn lens [x]
  (if (satisfies? Lens x) x
    (throw (#+clj IllegalArgumentException. #+cljs js/Error
            (str "Don't know how to create a lens from: " x)))))


(defn fetch [value seg]
  (-fetch (lens seg) value))

(defn putback [value seg subvalue]
  (-putback (lens seg) value subvalue))
  

(extend-type #+clj clojure.lang.Keyword #+cljs cljs.core.Keyword
    Lens
    (-fetch [seg value] (get value seg))
    (-putback [seg value subval] (assoc value seg subval)))

(extend-type #+clj clojure.lang.Symbol #+cljs cljs.core.Symbol
    Lens
    (-fetch [seg value] (get value seg))
    (-putback [seg value subval] (assoc value seg subval)))

(extend-type #+clj String #+cljs string
    Lens
    (-fetch [seg value] (get value seg))
    (-putback [seg value subval] (assoc value seg subval)))

(extend-type #+clj Number #+cljs number
    Lens
    (-fetch [seg value] (nth value seg))
    (-putback [seg value subval] (assoc value seg subval)))

(extend-type #+clj clojure.lang.PersistentVector #+cljs cljs.core/PersistentVector
    Lens
    (-fetch [seg value] (fetch-in value seg))
    (-putback [seg value subval] (putback-in value seg subval)))

(extend-type #+clj clojure.lang.PersistentList #+cljs cljs.core/List
    Lens
    (-fetch [seg value] (fetch-in value seg))
    (-putback [seg value subval] (putback-in value seg subval)))


(defn- bound [mn n mx]
  (-> n (max mn) (min mx)))

(defn spliceable? [x]
  (or (nil? x) (sequential? x)))

(defrecord Slice [from to]
  Lens 
  (-fetch [seg x]
    (let [n (count x)]
      (subvec x (bound 0 from n) (bound 0 to n))))
  (-putback [seg x v]
    (let [n (count x)]
      (-> x 
        (subvec 0 (bound 0 from n))
        (into (if (spliceable? v) v (list v)))
        (into (subvec x (bound 0 to n) n))))))

(defn slice [from to] (Slice. from to))

(defn slice? [seg] (instance? Slice seg))

(defn create-lens [fetch putback]
  (reify Lens
    (-fetch [seg value] (fetch seg value))
    (-putback [seg value subvalue] (putback seg value subvalue))))

(defmacro deflens [name doc? initial-args & methods]
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
               (fresnel.lenses/create-lens ~(:fetch methods) ~(:putback methods) )))]
    `(def ~name ~(if auto-segment (list f) f))))



(defn fetch-in [obj path]
  (reduce fetch obj path))


(defn putback-in [obj [seg & path] value]
  (when seg
    (if (empty? path)
      (putback obj seg value)
      (putback obj seg (putback-in (fetch obj seg) path value)))))

