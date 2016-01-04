(require 'cljs.build.api)

(def test-inputs (cljs.build.api/inputs "src/main/clojure" "src/test/clojure"))

(def test-opts
  {:main 'gpio.test-runner
   :output-to "target/out/test.js"
   :output-dir "target/out"
   :target :nodejs })

(require 'clojure.java.shell)

(defn run-tests []
  (let [result (clojure.java.shell/sh "node" (:output-to test-opts))]
    (println (:out result))
    (.println *err* (:err result))))

(cljs.build.api/build test-inputs test-opts)

(run-tests)

(System/exit 0)
