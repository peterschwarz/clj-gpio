(defproject clj-gpio "0.0.1-SNAPSHOT"
    :description "A lightweight Clojure library for Raspberry PI GPIO"
    :url "https://github.com/peterschwarz/clj-gpio"
    :min-lein-version  "2.0.0"
    :source-paths      ["src/main/clojure"]
    :test-paths        ["src/test/clojure"]
    :java-source-paths ["src/main/java"]
    :javac-options     ["-target" "1.6" "-source" "1.6"]

    :dependencies [[org.clojure/clojure "1.5.1"]
                   [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                   [net.java.dev.jna/jna "4.1.0"]])