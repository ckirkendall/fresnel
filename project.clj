(defproject fresnel "0.3.1-SNAPSHOT"
  :description "A library for composing lenses and working with complex state objects"
  :url "https://github.com/ckirkendall/fresnel"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.520"]]
  :source-paths ["target/classes"]
  :test-paths ["target/test-classes"]
  :doo {:build "test"
        :alias {:default [:node]}}
  :profiles {:dev {:dependencies [[lein-doo "0.1.10"]]
                   :plugins [[lein-cljsbuild "1.1.7"]
                             [lein-doo "0.1.10"]]
                   :cljsbuild {:builds [{:id "dev"
                                         :source-paths ["src" "test"]
                                         :compiler {:main          fresnel.runner
                                                    :output-to "target/main.js"
                                                    :optimizations :whitespace
                                                    :pretty-print true}}
                                        {:id "test"
                                         :source-paths ["src" "test"]
                                         :compiler {:main          fresnel.runner
                                                    :output-to     "target/doo/test.js"
                                                    :output-dir    "target/doo/out"
                                                    :target        :nodejs
                                                    :language-in   :ecmascript5
                                                    :optimizations :none}}]}}})
