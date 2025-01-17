(ns step-functions-receive-docstrings-as-strings-test
  (:require [burpless :refer [run-cucumber step]]
            [clojure.test :refer [deftest is]]))

(def step-functions-receive-docstrings-as-strings-steps
  [(step :Given "the expected doc-string value is {string}"
         (fn [state ^String expected-doc-string-value]
           (assoc state :expected-doc-string-value expected-doc-string-value)))

   (step :When "I extract the doc-string value from this step:"
         (fn [state ^String actual-doc-string-value]
           (assoc state :actual-doc-string-value actual-doc-string-value)))

   (step :Then "the actual extracted value should be of type {string}"
         (fn [state ^String expected-doc-string-value-type]
           (assoc state :expected-doc-string-value-type (Class/forName expected-doc-string-value-type))))

   (step :Then "the actual extracted doc-string value should match the expected doc-string value"
         (fn [{:keys [expected-doc-string-value actual-doc-string-value]}]
           (assert (= expected-doc-string-value actual-doc-string-value)
                   (str expected-doc-string-value " != " actual-doc-string-value))))])

(deftest step-functions-receive-docstrings-as-strings
  (is (zero? (run-cucumber "test-resources/features/docstrings-as-strings.feature"
                           step-functions-receive-docstrings-as-strings-steps))))
