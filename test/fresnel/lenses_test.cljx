(ns fresnel.lenses-test
  #+clj (:use clojure.test)
  (:require [fresnel.lenses :refer [#+clj deflens #+clj deffetch #+clj defputback 
                                    IFetch IPutback fetch putback create-lens slice]]
            [clojure.string :refer [split]]
            #+cljs [cemerick.cljs.test :as t])
  #+cljs (:require-macros [cemerick.cljs.test
                           :refer [is deftest testing are]]
                          [fresnel.lenses :refer [deflens deffetch defputback]]))

(deflens comma-to-map [oval nval]
  :fetch 
     (reduce #(assoc %1 (.trim %2) true) {} (split oval #","))
  :putback
  (reduce #(if %1 (str %2 "," %1) %2)
          nil
          (filter #(nval %1) (keys nval))))

(deftest fetch-tests
  (testing "fetching using simple keyword"
    (let [data {:a "success"}]
      (is (= "success" (fetch data :a)))))
  (testing "fetching using a string"
    (let [data {"a" "success"}]
      (is (= "success" (fetch data "a")))))
  (testing "fetching using a string"
    (let [data {'a "success"}]
      (is (= "success" (fetch data 'a)))))
  (testing "fetching using a number on a vector"
    (let [data ["a" "b" "c"]]
      (is (= "b" (fetch data 1)))))
  (testing "fetching using a number on a list"
    (let [data '("a" "b" "c")]
      (is (= "b" (fetch data 1)))))
  (testing "fetching using a simple custom segment"
    (let [seg (create-lens (fn [_ data] (nth data 1))
                              (fn [_ _ nval] nval))
          data ["a" "b" "c"]]
      (is (= "b" (fetch data seg)))))
  (testing "fetching using a complex segement"
    (let [data "a,b,c"]
      (is (= {"a" true
              "b" true
              "c" true}
             (fetch data comma-to-map)))))
  (testing "fetching using slice segment"
    (let [data [:a :b :c :d :e :f]]
      (is (= '(:c :d)
             (fetch data (slice 2 4)))))))


(deftest putback-tests
  (testing "putback using simple keyword"
    (let [data {:a "fail"}]
      (is (= {:a "success"} (putback data :a "success")))))
  (testing "putback using a string"
    (let [data {"a" "fail"}]
      (is (= {"a" "success"} (putback data "a" "success")))))
  (testing "putback using a string"
    (let [data {'a "fail"}]
      (is (= {'a "success"} (putback data 'a "success")))))
  (testing "putback using a number on a vector"
    (let [data ["a" "fail" "c"]]
      (is (= ["a" "b" "c"] (putback data 1 "b")))))
  (testing "putback using a number on a list"
    (let [data '("a" "fail" "c")]
      (is (= '("a" "b" "c") (putback data 1 "b")))))  (testing "putback using a custom segment"
    (let [seg (create-lens (fn [_ data] (nth data 1))
                           (fn [_ data nval] (assoc data 1 nval)))
          data ["a" "fail" "c"]]
      (is (= ["a" "b" "c"] (putback data seg "b")))))
  (testing "putback using a complex segement"
    (let [data "a,b,c"
          res (putback data
                           comma-to-map
                           (assoc (fetch data comma-to-map) "d" true))]
      (is (or  (= "a,b,c,d" res)
               (= "d,c,b,a" res)))))
  (testing "fetching using slice segment"
    (let [data [:a :b :c :d :e :f]]
      (is (= '(:a :b :t :t :t :e :f)
             (putback data (slice 2 4) [:t :t :t]))))))


(deftest fetch-in-tests
  (testing "basic compond map with keys"
    (let [data {:a {:b {"c" 1 :d 2} 'e 3} :f [1 2 3 4]}]
      (are [x y]  (= x (fetch data y))
           1 [:a :b "c"]
           3 [:a 'e]
           4 [:f 3]
           {"c" 1 :d 2} [:a :b])))
  (testing "compond map with complex segment"
    (let [data {:a ["x,y,z" "a,b,c"]}]
      (is (= true (fetch data [:a 1 comma-to-map "b"])))
      (is (= nil (fetch data [:a 1 comma-to-map "d"]))))))


(deftest putback-in-tests
  (testing "basic compond map with keys"
    (let [data {:a {:b {"c" 1 :d 2} 'e 3} :f [1 2 3 4]}]
      (is (= {:a {:b {"c" 1 :d 2} 'e 3} :f [1 2 5 4]}
             (putback data [:f 2] 5)))
      (is (= {:a {:b {"c" 1 :d 5} 'e 3} :f [1 2 3 4]}
             (putback data [:a :b :d] 5)))))
  (testing "compond map with complex segment"
    (let [data {:a ["x,y,z" "a,b,c"]}
          res (putback data [:a 1 comma-to-map "b"] false)]
      (is (or (= {:a ["x,y,z" "a,c"]} res)
              (= {:a ["x,y,z" "c,a"]} res))))))


(deffetch fetch-tmp [val]
  val)

(deftest deffetch-tests
  (testing "a deffetch reifies IFetch"
    (is (satisfies? IFetch fetch-tmp))
    (is (= (fetch 3 fetch-tmp) 3))))


(defputback putback-tmp [oval nval]
  (conj oval nval))

(deftest defputback-tests
  (testing "a defputback reifies IFetch"
    (is (satisfies? IPutback putback-tmp))
    (is (= (putback [] putback-tmp 3) [3]))))
