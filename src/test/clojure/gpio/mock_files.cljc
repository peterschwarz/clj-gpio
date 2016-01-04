(ns gpio.mock-files
  (:require [gpio.io]
            #?(:clj  [clojure.java.io :refer [file delete-file]]
               :cljs [cljs.nodejs :as nodejs])))

#?(:cljs
    (do
      (defonce ^:private fs (nodejs/require "fs"))
      (defonce ^:private path (nodejs/require "path"))))

(defn- dir? [f]
  #?(:clj (.isDirectory (file f))
     :cljs (let [stats (.statSync fs f)]
             (.isDirectory stats))))

(defn- in-parent [parent filename]
  (let [f (subs filename 1)]
    #?(:clj (file parent f)
       :cljs (.join path parent f))))

(defn- get-parent [f]
  #?(:clj (.getParentFile f)
     :cljs (.dirname path f)))

(defn- list-files [f]
  #?(:clj (.listFiles (file f))
     :cljs (->> (.readdirSync fs f)
                (map #(.join path f %)))))

(defn- fexists? [f]
  #?(:clj (.exists (file f))
     :cljs (try 
             (.closeSync fs (.openSync fs f "r"))
             true
             (catch :default e
               false))))

(defn- del-file [f]
  #?(:clj (delete-file (file f))
     :cljs (when (fexists? f)
             (if (dir? f)
               (.rmdirSync fs f)
               (.unlinkSync fs f)))))

(defn delete-recursively  [fname]
    (let [func (fn [func f] 
                 (when (and (fexists? f) (dir? f))
                   (doseq [f2 (list-files f)]
                     (func func f2)))
                 (del-file f))]
      (func func fname)))

(defn- mkdirs [f]
 #?(:clj (.mkdirs (file f))
    :cljs (let [parts (.split f (.-sep path))]
            (loop [dir (first parts)
                   remaining (rest parts)]
             (if (fexists? dir)
               (if (not (empty? remaining))
                 (recur (.join path dir (first remaining))
                        (rest remaining))
                 true)
               (try
                 (.mkdirSync fs dir)
                 (if (not (empty? remaining))
                   (recur (.join path dir (first remaining))
                          (rest remaining))
                   true)
                 (catch :default e
                   false)))))))

(defn- spitp [spit-fn parent filename content]
  (let [f (in-parent parent filename)]
    (-> (get-parent f)
        (mkdirs))
    (spit-fn f content)))

(defn- slurpp [slurp-fn parent filename]
    (slurp-fn (in-parent parent filename)))


(defn with-mock-files [f]
  (let [test-dir "target/test-files"
        has-test-dir? (or (fexists? test-dir) (mkdirs test-dir))]
     (assert has-test-dir? "unable to create test directory")

     (let [orig-spit gpio.io/write-file
           orig-slurp gpio.io/read-file]
       (with-redefs [gpio.io/write-file (partial spitp orig-spit test-dir)
                     gpio.io/read-file (partial slurpp orig-slurp test-dir)]
         (f)))))

(defn mock-file-fixture [f]
  (with-mock-files f) 
  (delete-recursively "target/test-files"))
