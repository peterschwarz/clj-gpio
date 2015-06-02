(ns gpio-core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer :all]
            [gpio.core :refer :all]))

(deftest test-high-low-value
  (testing "high"
    (is (= (byte \1) (high-low-value :high)))
    (is (= (byte \1) (high-low-value 'high)))
    (is (= (byte \1) (high-low-value 1)))
    (is (= (byte \1) (high-low-value "1")))
    (is (= (byte \1) (high-low-value \1))))

  (testing "low"
    (is (= (byte \0) (high-low-value :low)))
    (is (= (byte \0) (high-low-value 'low)))
    (is (= (byte \0) (high-low-value 0)))
    (is (= (byte \0) (high-low-value "0")))
    (is (= (byte \0) (high-low-value \0))))

  (testing "invalid values"
    (is (thrown? AssertionError (high-low-value 3)))
    (is (thrown? AssertionError (high-low-value :foo)))))

(defn- in-parent [parent filename]
  (file parent (subs filename 1)))

(defn spitp [spit-fn parent filename content]
  (let [f (in-parent parent filename)]
    (-> (.getParentFile f)
        (.mkdirs))
    (spit-fn f content)))

(defn slurpp [slurp-fn parent filename]
    (slurp-fn (in-parent parent filename)))

(defn random-accessp [random-access-fn parent filename]
  (random-access-fn (in-parent parent filename)))

(defmacro with-mock-files [& body]
  `(let [test-dir# (as-file "target/test-files")
         exists-or-created# (or (.exists test-dir#) (.mkdirs test-dir#))]
    (assert exists-or-created# "unable to create test directory")

    (let [orig-spit# spit
          orig-slurp# slurp
          orig-random-access# random-access]
      (with-redefs [spit (partial spitp orig-spit# test-dir#)
                    slurp (partial slurpp orig-slurp# test-dir#)
                    random-access (partial random-accessp orig-random-access# test-dir#)]
        ~@body))))

(deftest test-export
  (with-mock-files
    (export! 17)
    (is (= "17" (slurp "/sys/class/gpio/export")))))

(deftest test-unexport
  (with-mock-files
    (unexport! 18)
    (is (= "18" (slurp "/sys/class/gpio/unexport")))))

(deftest test-open-port
  (with-mock-files
    (spit "/sys/class/gpio/gpio17/value" \0)
    (let [port (open-port 17)]
      (is (= :low (read-value port)))
      )))
