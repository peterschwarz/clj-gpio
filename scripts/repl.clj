(require 'cljs.repl)
(require 'cljs.build.api)
(require 'cljs.repl.node)

(cljs.build.api/build "src/main/clojure"
  {:output-to "target/out/main.js"
   :output-dir "target/out"
   :verbose true})

(cljs.repl/repl (cljs.repl.node/repl-env)
  :watch "src/main/clojure"
  :output-dir "target/out")
