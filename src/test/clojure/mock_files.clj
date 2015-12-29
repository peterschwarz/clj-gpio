(ns mock-files
  (:require [clojure.java.io
             :refer [file delete-file]]))

(defn delete-recursively  [fname]
    (let [func (fn [func f] 
                 (when  (.isDirectory f)
                   (doseq  [f2  (.listFiles f)]
                     (func func f2)))
                 (delete-file f))]
      (func func  (file fname))))

(defn- in-parent [parent filename]
  (file parent (subs filename 1)))

(defn- spitp [spit-fn parent filename content]
  (let [f (in-parent parent filename)]
    (-> (.getParentFile f)
        (.mkdirs))
    (spit-fn f content)))

(defn- slurpp [slurp-fn parent filename]
    (slurp-fn (in-parent parent filename)))


(defmacro with-mock-files [& body]
  `(let [test-dir# (file "target/test-files")
         exists-or-created# (or (.exists test-dir#) (.mkdirs test-dir#))]
    (assert exists-or-created# "unable to create test directory")

    (let [orig-spit# spit
          orig-slurp# slurp]
      (with-redefs [spit (partial spitp orig-spit# test-dir#)
                    slurp (partial slurpp orig-slurp# test-dir#)]
        ~@body))))

(defn mock-file-fixture [f]
  (with-mock-files
    (f)
    ) 
  (delete-recursively "target/test-files"))
