(ns segments-test
  (:use clojure.test)
  (:require [segments :refer [defsegment Segment putback-in
                              fetch-in fetch putback create-segment]]))


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
  (testing "fetching using a number"
    (let [data ["a" "b" "c"]]
      (is (= "b" (fetch data 1)))))
  (testing "fetching using a custom segment"
    (let [seg (create-segment (fn [_ data] (nth data 1))
                              (fn [_ _ nval] nval))
          data ["a" "b" "c"]]
      (is (= "b" (fetch data seg))))))


(deftest putback-tests
  (testing "fetching using simple keyword"
    (let [data {:a "fail"}]
      (is (= {:a "success"} (putback data :a "success")))))
  (testing "fetching using a string"
    (let [data {"a" "fail"}]
      (is (= {"a" "success"} (putback data "a" "success")))))
  (testing "fetching using a string"
    (let [data {'a "fail"}]
      (is (= {'a "success"} (putback data 'a "success")))))
  (testing "fetching using a number"
    (let [data ["a" "fail" "c"]]
      (is (= ["a" "b" "c"] (putback data 1 "b")))))
  (testing "fetching using a custom segment"
    (let [seg (create-segment (fn [_ data] (nth data 1))
                              (fn [_ data nval] (assoc data 1 nval)))
          data ["a" "fail" "c"]]
      (is (= ["a" "b" "c"] (putback data seg "b"))))))
