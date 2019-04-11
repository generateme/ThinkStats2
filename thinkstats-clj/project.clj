(defproject thinkstats-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [generateme/fastmath "1.3.0-SNAPSHOT"]
                 [cljplot "0.0.1-SNAPSHOT"]
                 [clojure.java-time "0.3.2"]
                 [org.clojure/data.csv "0.1.4"]
                 [tech.tablesaw/tablesaw-core "0.32.6"]
                 [org.clojure/data.json "0.2.6"]
                 [marginalia "0.9.1" :exclusions [org.clojure/tools.reader
                                                  org.clojure/tools.namespace
                                                  org.clojure/clojurescript
                                                  org.clojure/google-closure-library
                                                  org.clojure/google-closure-library-third-party]]]
  :repl-options {:timeout 120000})
