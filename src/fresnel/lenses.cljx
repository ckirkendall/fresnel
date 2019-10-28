(ns fresnel.lenses
  (:refer-clojure :exclude [key]))

(declare fetch-in putback-in)

(defprotocol IFetch
  (-fetch [seg value]))

(defprotocol IPutback
  (-putback [seg value subvalue]))


(defn fetch-lens [x]
  (if (satisfies? IFetch x) x
      (throw (#+clj IllegalArgumentException. #+cljs js/Error
                    (str "Lens does not satisfy IFetch protocol: " (pr-str x))))))

(defn putback-lens [x]
  (if (satisfies? IPutback x) x
      (throw (#+clj IllegalArgumentException. #+cljs js/Error
                    (str "Lens does not satisfy IPutback protocol: " (pr-str x))))))

(defn bi-lens [x]
  (if (and (fetch-lens x)
           (putback-lens x))
    x))

(defn fetch [value seg]
  (try (-fetch seg value)
       (catch #+clj IllegalArgumentException
         #+cljs js/Error e
         (fetch-lens seg)
         (throw e))))

(defn putback [value seg subvalue]
  (-putback (putback-lens seg) value subvalue))


;; This is used to dissoc a key in an associtive structure.
;; if used as the value of putback it will trigger a
;; dissoc on the seg.
(def dissoc-trigger :fresnel/dissoc)


(defn safe-assoc [value seg sub-val]
  (if (identical? sub-val dissoc-trigger)
    (dissoc value seg)
    (assoc value seg sub-val)))


(defprotocol ISafeNth
  (-safe-nth [value n]))

(defprotocol ISafeNumAssoc
  (-safe-num-assoc [value seg sub-val]))


(extend-type #+clj clojure.lang.Keyword #+cljs cljs.core.Keyword
             IFetch
             (-fetch [seg value] (get value seg))
             IPutback
             (-putback [seg value subval]
               (let [val (if (nil? value) {} value)]
                 (safe-assoc val seg subval))))

(extend-type #+clj clojure.lang.Symbol #+cljs cljs.core.Symbol
             IFetch
             (-fetch [seg value] (get value seg))
             IPutback
             (-putback [seg value subval]
               (let [val (if (nil? value) {} value)]
                 (safe-assoc val seg subval))))

(extend-type #+clj String #+cljs string
             IFetch
             (-fetch [seg value] (get value seg))
             IPutback
             (-putback [seg value subval]
               (let [val (if (nil? value) {} value)]
                 (safe-assoc val seg subval))))

(extend-type #+clj Number #+cljs number
             IFetch
             (-fetch [seg value] (when (not (nil? value))
                                   (-safe-nth value seg)))
             IPutback
             (-putback [seg value subval]
               (let [val (if (nil? value) [] value)]
                 (-safe-num-assoc value seg subval))))

(extend-type #+clj clojure.lang.PersistentVector #+cljs cljs.core/PersistentVector
             IFetch
             (-fetch [seg value] (fetch-in value seg))
             IPutback
             (-putback [seg value subval] (putback-in value seg subval))
             ISafeNth
             (-safe-nth [value seg] (nth value seg))
             ISafeNumAssoc
             (-safe-num-assoc [value seg sub-value] (assoc value seg sub-value)))

(extend-type #+clj clojure.lang.PersistentList #+cljs cljs.core/List
             IFetch
             (-fetch [seg value] (fetch-in value seg))
             IPutback
             (-putback [seg value subval] (putback-in value seg subval)))

(extend-type #+clj clojure.lang.PersistentArrayMap #+cljs cljs.core/PersistentArrayMap
             ISafeNth
             (-safe-nth [value seg] (get value seg))
             ISafeNumAssoc
             (-safe-num-assoc [value seg sub-value] (safe-assoc value seg sub-value)))

(extend-type #+clj clojure.lang.PersistentHashMap #+cljs cljs.core/PersistentHashMap
             ISafeNth
             (-safe-nth [value seg] (get value seg))
             ISafeNumAssoc
             (-safe-num-assoc [value seg sub-value] (safe-assoc value seg sub-value)))


(extend-type #+clj Object #+cljs object
             ISafeNth
             (-safe-nth [value seg]
               (if (vector? value)
                 (get value seg)
                 (loop [idx 0 val value]
                   (if (or (empty? val) (>= idx seg))
                     (first val)
                     (recur (inc idx) (rest val))))))
             ISafeNumAssoc
             (-safe-num-assoc [value seg sub-val]
               (cond
                 (vector? value)
                 (assoc value seg sub-val)

                 (map? value)
                 (safe-assoc value seg sub-val)

                 :else ;seq
                 (loop [idx 0 val value accum []]
                   (cond
                     (>= idx seg) (concat (conj accum sub-val) (rest val))
                     (empty? val) (recur (inc idx) val (conj accum nil))
                     :else (recur (inc idx) (rest val) (conj accum (first val))))))))

(defn key [ky]
  (reify
    IFetch
    (-fetch [seg value] (get value ky))
    IPutback
    (-putback [seg value subval]
      (let [val (if (nil? value) {} value)]
        (assoc val ky subval)))))

(defn- bound [mn n mx]
  (-> n (max mn) (min mx)))

(defn spliceable? [x]
  (or (nil? x) (sequential? x)))

(defrecord Slice [from to]
  IFetch
  (-fetch [seg x]
    (let [n (count x)]
      (subvec x (bound 0 from n) (bound 0 to n))))
  IPutback
  (-putback [seg x v]
    (let [n (count x)]
      (-> x
          (subvec 0 (bound 0 from n))
          (into (if (spliceable? v) v (list v)))
          (into (subvec x (bound 0 to n) n))))))

(defn slice [from to] (Slice. from to))

(defn slice? [seg] (instance? Slice seg))


(defn create-fetch-lens [fetch-fn]
  (reify
    IFetch
    (-fetch [seg value] (fetch-fn seg value))))


(defn create-putback-lens [putback-fn]
  (reify
    IPutback
    (-putback [seg value subvalue] (putback-fn seg value subvalue))))


(defn create-bi-lens [fetch-fn putback-fn]
  (reify
    IFetch
    (-fetch [seg value] (fetch-fn seg value))
    IPutback
    (-putback [seg value subvalue] (putback-fn seg value subvalue))))


(defn create-lens [fetch-fn putback-fn]
  {:pre [(or fetch putback)]}
  (cond
    (and fetch-fn putback-fn)
    (create-bi-lens fetch-fn putback-fn)

    fetch-fn
    (create-fetch-lens fetch-fn)

    putback-fn
    (create-putback-lens putback-fn)))


(defmacro deffetch [name doc? initial-args & methods]
  (let [[name initial-args methods] (if (string? doc?)
                                      [(vary-meta name assoc :doc doc?) initial-args methods]
                                      [name doc? (cons initial-args methods)])
        [value-arg & args] initial-args]
    `(deflens ~name [~value-arg nil ~@args] :fetch ((fn [] ~@methods)))))


(defmacro defputback [name doc? initial-args & methods]
  (let [[name initial-args methods] (if (string? doc?)
                                      [(vary-meta name assoc :doc doc?) initial-args methods]
                                      [name doc? (cons initial-args methods)])]
    `(deflens ~name ~initial-args :putback ((fn [] ~@methods)))))


(defmacro deflens [name doc? initial-args & methods]
  (let [[name initial-args methods] (if (string? doc?)
                                      [(vary-meta name assoc :doc doc?) initial-args methods]
                                      [name doc? (cons initial-args methods)])
        [value-arg subvalue-arg & args] initial-args
        auto-segment (empty? args)
        [args [_ & rest-arg]] (split-with #(not= (clojure.core/name %) "&") args)
        plain-args (take (count args) (repeatedly gensym))
        plain-rest-arg (when rest-arg (gensym))
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
