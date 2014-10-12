(defproject fresnel "0.3.0-SNAPSHOT"
  :description "A library for composing lenses and working with complex state objects"
  :url "https://github.com/ckirkendall/fresnel"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2138"]]
  :source-paths ["target/classes"]
  :test-paths ["target/test-classes"]
  :profiles {:dev {:plugins [[com.keminglabs/cljx "0.3.2"]
                             [com.cemerick/austin "0.1.3"]
                             [com.cemerick/clojurescript.test "0.2.1"]
                             [lein-cljsbuild "1.0.2"]]
                   :hooks [cljx.hooks]
                   :cljx {:builds [{:source-paths ["src"]
                                    :output-path "target/classes"
                                    :rules :clj}
                                   {:source-paths ["src"]
                                    :output-path "target/classes"
                                    :rules :cljs}
                                   {:source-paths ["test"]
                                    :output-path "target/test-classes"
                                    :rules :clj}
                                   {:source-paths ["test"]
                                    :output-path "target/test-classes"
                                    :rules :cljs}]}
                   :cljsbuild {:builds [{
                               :source-paths ["target/classes" "target/test-classes"]
                               :compiler {
                                          :output-to "target/main.js" 
                                          :optimizations :whitespace
                                          :pretty-print true}
                                         }]
                               :test-commands {"unit-tests" ["phantomjs"
                                                             :runner
                                                             "target/main.js"]}}}})

