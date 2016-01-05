(defproject clj-gpio "0.2.0"
  :description "A lightweight Clojure library for Raspberry PI GPIO"
  :url "http://peterschwarz.github.io/clj-gpio"
  :license  {:name "Eclipse Public License"
             :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version  "2.0.0"
  :source-paths      ["src/main/clojure"]
  :test-paths        ["src/test/clojure"]
  :java-source-paths ["src/main/java"]
  :javac-options     ["-target" "1.6" "-source" "1.6"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"]
                 [net.java.dev.jna/jna "4.2.1"]]

  :aliases {"cljs:test" ["trampoline" "run"  "-m" "clojure.main" "./scripts/test.clj"]
            "cljs:repl" ["trampoline" "run"  "-m" "clojure.main" "./scripts/repl.clj"]
            "cljs:dev" ["trampoline" "run"  "-m" "clojure.main" "./scripts/build.clj"]
            "cljs:dev:watch" ["trampoline" "run"  "-m" "clojure.main" "./scripts/watch.clj"]
            "cleantest" ["do" ["clean"] ["test"] ["cljs:test"]]})

