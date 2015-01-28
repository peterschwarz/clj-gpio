(defproject clj-gpio "0.1.0-SNAPSHOT"
    :description "A lightweight Clojure library for Raspberry PI GPIO"
    :url "https://github.com/peterschwarz/clj-gpio"
    :min-lein-version  "2.0.0"
    :source-paths      ["src/main/clojure"]
    :test-paths        ["src/test/clojure"]
    :java-source-paths ["src/main/java"]
    :javac-options     ["-target" "1.6" "-source" "1.6"]

    :dependencies [[org.clojure/clojure "1.6.0"]
                   [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                   [net.java.dev.jna/jna "4.1.0"]])