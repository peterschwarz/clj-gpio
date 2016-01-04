(require 'cljs.build.api)

(cljs.build.api/build
  (cljs.build.api/inputs "src/main/clojure" "src/dev/clojure")
  {:main 'gpio.dev
   :output-to "target/out/dev.js"
   :output-dir "target/out"
   :target :nodejs })
