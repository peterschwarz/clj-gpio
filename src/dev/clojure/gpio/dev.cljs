(ns gpio.dev
  (:require [cljs.nodejs :as nodejs]
            [gpio.core :as gpio]
            [cljs.core.async :as a :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(nodejs/enable-util-print!)


(defn- configure-ports [led btn]
  (println "Configuring ports")
  (gpio/set-direction! led :out)

  (gpio/set-direction! btn :in)
  (gpio/set-active-low! btn true)
  (gpio/set-edge! btn :both))

(defn- forward-value [ch led]
  (go
    (println "Looping on btn changes...")
    (loop []
      (when-let [value (<! ch)]
        (gpio/write-value! led value)
        (recur)))))

(defn- on-sigint [led btn]
  (println "\nCleaning up ports")
  (gpio/close! led)
  (gpio/close! btn))

(defn setup-led-and-button [led-pin button-pin]
  (let [led (gpio/open-port led-pin)
        btn (gpio/open-channel-port button-pin)
        ch (gpio/create-edge-channel btn)]

    (.on js/process "SIGINT" #(on-sigint led btn))

    (println "Waiting to settle")
    (js/setTimeout (fn []
                     (configure-ports led btn)
                     (forward-value ch led))
                   100)))

(defn -main [& args]
  (let [args (into [] args)
        led (get args 0 18)
        button (get args 1 19)]
    (println "Running against an LED on pin" led "and a button on pin" button)
    (setup-led-and-button led button)))

(set! *main-cli-fn* -main)
