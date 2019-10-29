(defproject fresnel "0.3.1-SNAPSHOT"
  :description "A library for composing lenses and working with complex state objects"
  :url "https://github.com/ckirkendall/fresnel"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.520"]]
  :plugins [[lein-shell "0.5.0"]]
  :source-paths ["src"]
  :test-paths ["src" "test"]
  :doo {:build "test"
        :alias {:default [:node]}}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["shell" "echo" "bumped version"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v" "--no-sign"] ; disable signing and add "v" prefix
                  ["shell" "echo" "tagged version"]
                  ["deploy" "clojars"]
                  ["shell" "echo" "deployed"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["shell" "echo" "bump version back to snapshot"]
                  ["vcs" "push"]
                  ["shell" "echo" "pushed"]]
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
