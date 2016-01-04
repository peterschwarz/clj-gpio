(ns gpio.io
  #?(:cljs (:require [cljs.nodejs :as nodejs])))

#?(:cljs (defonce ^:private fs (nodejs/require "fs")))

(defn write-file [filename content]
  #?(:clj  (spit filename content)
     :cljs (.writeFileSync fs filename content #js {:encoding "ascii"})))

(defn read-file [filename]
  #?(:clj  (slurp filename)
     :cljs (.readFileSync fs filename #js {:encoding "ascii"})))
