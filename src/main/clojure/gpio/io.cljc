(ns gpio.io
  #?(:cljs (:require [cljs.nodejs :as nodejs])))

#?(:cljs (defonce ^:private fs (nodejs/require "fs")))
#?(:cljs (defonce ^:private file-opts #js {:encoding "ascii"}))

(defn write-file
  ([filename content]
  #?(:clj  (spit filename content)
     :cljs (.writeFileSync fs filename content file-opts)))
  #?(:cljs ([filename content cb]
            (.writeFile fs filename content file-opts #(cb %)))))

(defn read-file
  ([filename]
  #?(:clj  (slurp filename)
     :cljs (.readFileSync fs filename file-opts)))
  #?(:cljs ([filename cb]
            (.readFile fs filename file-opts #(cb %1 %2)))))
