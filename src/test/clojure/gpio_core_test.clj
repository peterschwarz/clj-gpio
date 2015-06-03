(ns gpio-core-test
  (:require [clojure.test :refer :all]
            [mock-files :refer :all]
            [gpio.core :refer :all]))

(deftest test-high-low-value
  (testing "high"
    (is (= (byte \1) (high-low-value :high)))
    (is (= (byte \1) (high-low-value 'high)))
    (is (= (byte \1) (high-low-value 1)))
    (is (= (byte \1) (high-low-value "1")))
    (is (= (byte \1) (high-low-value \1)))
    (is (= (byte \1) (high-low-value true))))

  (testing "low"
    (is (= (byte \0) (high-low-value :low)))
    (is (= (byte \0) (high-low-value 'low)))
    (is (= (byte \0) (high-low-value 0)))
    (is (= (byte \0) (high-low-value "0")))
    (is (= (byte \0) (high-low-value \0)))
    (is (= (byte \0) (high-low-value false))))

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

(deftest test-read-symbol
  (spit "/sys/class/gpio/gpio2/value" \1)
  (let [port (open-port 2 :digital-result-format :symbol)]
    (is (= 'high (read-value port)))
    (write-value! port :low)
    (is (= 'low (read-value port)))))

(deftest test-read-boolean
  (spit "/sys/class/gpio/gpio2/value" \1)
  (let [port (open-port 2 :digital-result-format :boolean)]
    (is (= true (read-value port)))
    (write-value! port :low)
    (is (= false (read-value port)))))

(deftest test-read-integer
  (spit "/sys/class/gpio/gpio2/value" \1)
  (let [port (open-port 2 :digital-result-format :integer)]
    (is (= 1 (read-value port)))
    (write-value! port :low)
    (is (= 0 (read-value port)))))

(deftest test-read-char
  (spit "/sys/class/gpio/gpio2/value" \1)
  (let [port (open-port 2 :digital-result-format :char)]
    (is (= \1 (read-value port)))
    (write-value! port :low)
    (is (= \0 (read-value port)))))

(deftest test-read-custom
  (spit "/sys/class/gpio/gpio2/value" \1)
  (let [port (open-port 2 :from-raw-fn #(if (= \1 %) :foo :bar))]
    (is (= :foo (read-value port)))
    (write-value! port :low)
    (is (= :bar (read-value port)))))

(deftest test-write-keyword
  (spit "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port :high)
    (is (= "1" (slurp "/sys/class/gpio/gpio17/value")))
    (write-value! port :low)
    (is (= "0" (slurp "/sys/class/gpio/gpio17/value")))))
 
(deftest test-write-symbol
  (spit "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port 'high)
    (is (= "1" (slurp "/sys/class/gpio/gpio17/value")))
    (write-value! port 'low)
    (is (= "0" (slurp "/sys/class/gpio/gpio17/value")))))

(deftest test-write-character
  (spit "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port \1)
    (is (= "1" (slurp "/sys/class/gpio/gpio17/value")))
    (write-value! port \0)
    (is (= "0" (slurp "/sys/class/gpio/gpio17/value")))))

(deftest test-write-integer
  (spit "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port 1)
    (is (= "1" (slurp "/sys/class/gpio/gpio17/value")))
    (write-value! port 0)
    (is (= "0" (slurp "/sys/class/gpio/gpio17/value")))))

(deftest test-write-string
  (spit "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port "1")
    (is (= "1" (slurp "/sys/class/gpio/gpio17/value")))
    (write-value! port "0")
    (is (= "0" (slurp "/sys/class/gpio/gpio17/value")))))

(deftest test-write-boolean
  (spit "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port true)
    (is (= "1" (slurp "/sys/class/gpio/gpio17/value")))
    (write-value! port false)
    (is (= "0" (slurp "/sys/class/gpio/gpio17/value")))))

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
