(ns gpio-core-test
  (:require [clojure.test :refer :all]
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
    (is (thrown? AssertionError (high-low-value :foo))))
  )
