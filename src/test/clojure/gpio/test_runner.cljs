(ns gpio.test-runner
  (:require [cljs.nodejs :as nodejs]
            [cljs.test :refer-macros  [run-tests]]
            ; Insert more test ns's here
            [gpio.core-tests]))

(nodejs/enable-util-print!)

(defn run-suite []
  (run-tests
    ; Insert more test ns's here
    'gpio.core-tests))

(defn- report-display [m]
  {:title (str "CLJS tests: " (if (cljs.test/successful? m) "Success" "Failure"))
   :message (str "Ran " (:test m) " tests containing " (:pass m) " assertions.\n"
                 (:fail m) " failures, " (:error m) " errors.")})

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (let [report (report-display m)]
    (try
      (let [notifier (nodejs/require "node-notifier")]
        (.notify notifier
           #js {:title (:title report)
                :message (:message report)}))
      (catch :default e
        (println "WARN Unable to use node-notifier")
        (println "\nTo install run: \n$ npm install --save-dev node-notifier")
        ))))

(defn -main [& args]
  (run-suite))

(set! *main-cli-fn* -main)
