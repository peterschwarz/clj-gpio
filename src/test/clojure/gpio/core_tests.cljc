(ns gpio.core-tests
  (:require #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is testing use-fixtures]])
            [gpio.mock-files :refer [mock-file-fixture]]
            [gpio.io :refer [read-file write-file]]
            [gpio.core :refer [high-low-value
                               export! unexport! close!
                               open-port read-value write-value! toggle!
                               set-direction! set-active-low!
                               open-channel-port set-edge!]]))

(deftest test-high-low-value
  (testing "high"
    (is (= \1 (high-low-value :high)))
    (is (= \1 (high-low-value 'high)))
    (is (= \1 (high-low-value 1)))
    (is (= \1 (high-low-value "1")))
    (is (= \1 (high-low-value \1)))
    (is (= \1 (high-low-value true))))

  (testing "low"
    (is (= \0 (high-low-value :low)))
    (is (= \0 (high-low-value 'low)))
    (is (= \0 (high-low-value 0)))
    (is (= \0 (high-low-value "0")))
    (is (= \0 (high-low-value \0)))
    (is (= \0 (high-low-value false))))

  (testing "invalid values"
    (is (thrown? #?(:clj AssertionError :cljs js/Error) (high-low-value 3)))
    (is (thrown? #?(:clj AssertionError :cljs js/Error) (high-low-value :foo)))))

(use-fixtures :each mock-file-fixture)

(deftest test-export
  (export! 17)
  (is (= "17" (read-file "/sys/class/gpio/export"))))

(deftest test-unexport
  (unexport! 18)
  (is (= "18" (read-file "/sys/class/gpio/unexport"))))

(deftest test-open-port
  (write-file "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (is (= :low (read-value port)))
    (write-value! port :high)
    (is (= "1" (read-file "/sys/class/gpio/gpio17/value")))))

(deftest test-read-symbol
  (write-file "/sys/class/gpio/gpio2/value" \1)
  (let [port (open-port 2 :digital-result-format :symbol)]
    (is (= 'high (read-value port)))
    (write-value! port :low)
    (is (= 'low (read-value port)))))

(deftest test-read-boolean
  (write-file "/sys/class/gpio/gpio2/value" \1)
  (let [port (open-port 2 :digital-result-format :boolean)]
    (is (= true (read-value port)))
    (write-value! port :low)
    (is (= false (read-value port)))))

(deftest test-read-integer
  (write-file "/sys/class/gpio/gpio2/value" \1)
  (let [port (open-port 2 :digital-result-format :integer)]
    (is (= 1 (read-value port)))
    (write-value! port :low)
    (is (= 0 (read-value port)))))

(deftest test-read-char
  (write-file "/sys/class/gpio/gpio2/value" \1)
  (let [port (open-port 2 :digital-result-format :char)]
    (is (= \1 (read-value port)))
    (write-value! port :low)
    (is (= \0 (read-value port)))))

(deftest test-read-custom
  (write-file "/sys/class/gpio/gpio2/value" \1)
  (let [port (open-port 2 :from-raw-fn #(if (= \1 (first %)) :foo :bar))]
    (is (= :foo (read-value port)))
    (write-value! port :low)
    (is (= :bar (read-value port)))))

(deftest test-write-keyword
  (write-file "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port :high)
    (is (= "1" (read-file "/sys/class/gpio/gpio17/value")))
    (write-value! port :low)
    (is (= "0" (read-file "/sys/class/gpio/gpio17/value")))))
 
(deftest test-write-symbol
  (write-file "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port 'high)
    (is (= "1" (read-file "/sys/class/gpio/gpio17/value")))
    (write-value! port 'low)
    (is (= "0" (read-file "/sys/class/gpio/gpio17/value")))))

(deftest test-write-character
  (write-file "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port \1)
    (is (= "1" (read-file "/sys/class/gpio/gpio17/value")))
    (write-value! port \0)
    (is (= "0" (read-file "/sys/class/gpio/gpio17/value")))))

(deftest test-write-integer
  (write-file "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port 1)
    (is (= "1" (read-file "/sys/class/gpio/gpio17/value")))
    (write-value! port 0)
    (is (= "0" (read-file "/sys/class/gpio/gpio17/value")))))

(deftest test-write-string
  (write-file "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port "1")
    (is (= "1" (read-file "/sys/class/gpio/gpio17/value")))
    (write-value! port "0")
    (is (= "0" (read-file "/sys/class/gpio/gpio17/value")))))

(deftest test-write-boolean
  (write-file "/sys/class/gpio/gpio17/value" \0)
  (let [port (open-port 17)]
    (write-value! port true)
    (is (= "1" (read-file "/sys/class/gpio/gpio17/value")))
    (write-value! port false)
    (is (= "0" (read-file "/sys/class/gpio/gpio17/value")))))

(deftest test-set-direction
  (write-file "/sys/class/gpio/gpio19/value" \0)
  (let [port (open-port 19)]
    (set-direction! port :in)
    (is (= "in" (read-file "/sys/class/gpio/gpio19/direction")))))

(deftest test-set-active-low
  (write-file "/sys/class/gpio/gpio21/value" \0)
  (let [port (open-port 21)]
    (set-active-low! port true)
    (is (= "1" (read-file "/sys/class/gpio/gpio21/active_low")))))

(deftest test-toggle
  (write-file "/sys/class/gpio/gpio21/value" \0)
  (let [port (open-port 21)]
    (toggle! port)
    (is (= "1" (read-file "/sys/class/gpio/gpio21/value")))
    (toggle! port)
    (is (= "0" (read-file "/sys/class/gpio/gpio21/value")))))

(deftest test-close!
  (testing "close GpioPort"
   (let [port (open-port 21)]
     (close! port)
     (is (= "21" (read-file "/sys/class/gpio/unexport")))))
  
  #_(testing "close EdgeGpioPort"
    (let [port (open-channel-port 20)]
      (close! port)
     (is (= "20" (read-file "/sys/class/gpio/unexport"))))))

; Needs a platform-independent method for file watching
#_(deftest test-open-channel-port
  (write-file "/sys/class/gpio/gpio1/value" \0)
  (let [port (open-channel-port 1)]
    (set-edge! port :rising)
    (is (= "rising" (read-file "/sys/class/gpio/gpio1/edge")))))
