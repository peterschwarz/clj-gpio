(ns gpio-core-test
  (:require [clojure.test :refer :all]
            [file-utils :refer :all]
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

(use-fixtures :each mock-file-fixture)

(deftest test-export
  (export! 17)
  (is (= "17" (slurp "/sys/class/gpio/export"))))

(deftest test-unexport
  (unexport! 18)
  (is (= "18" (slurp "/sys/class/gpio/unexport"))))

(deftest test-open-port
  (spit "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (is (= :low (read-value port)))
    (write-value! port :high)
    (is (= "1" (slurp "/sys/class/gpio/gpio17/value")))))

(deftest test-set-direction
  (spit "/sys/class/gpio/gpio19/value" \0)
  (let [port (open-port 19)]
    (set-direction! port :in)
    (is (= "in" (slurp "/sys/class/gpio/gpio19/direction")))))

(deftest test-set-active-low
  (spit "/sys/class/gpio/gpio21/value" \0)
  (let [port (open-port 21)]
    (set-active-low! port true)
    (is (= "1" (slurp "/sys/class/gpio/gpio21/active_low")))))

; Needs a platform-independent method for file watching
#_(deftest test-open-channel-port
  (spit "/sys/class/gpio/gpio1/value" \0)
  (let [port (open-channel-port 1)]
    (set-edge! port :rising)
    (is (= "rising" (slurp "/sys/class/gpio/gpio1/edge")))))
