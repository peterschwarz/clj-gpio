(defproject clj-gpio "0.2.0-SNAPSHOT"
    :description "A lightweight Clojure library for Raspberry PI GPIO"
    :url "http://peterschwarz.github.io/clj-gpio"
    :license  {:name "Eclipse Public License"
               :url "http://www.eclipse.org/legal/epl-v10.html"}
    :min-lein-version  "2.0.0"
    :source-paths      ["src/main/clojure"]
    :test-paths        ["src/test/clojure"]
    :java-source-paths ["src/main/java"]
    :javac-options     ["-target" "1.6" "-source" "1.6"]

    :dependencies [[org.clojure/clojure "1.7.0"]
                   [org.clojure/core.async "0.2.374"]
                   [net.java.dev.jna/jna "4.2.1"]])

