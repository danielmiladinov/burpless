(ns clojure-test-assertion-macros-test
  (:require [clojure.test :refer [deftest is]]
            [burpless :refer [run-cucumber step]]
            [burpless.runtime :as runtime]))

(def steps
  [(step :Given "I have a number {long}"
         (fn [state ^Long num]
           (assoc state :actual num)))

   (step :Then "the number should be {long}"
         (fn [{:keys [actual] :as state} ^Long expected]
           (is (= expected actual))
           state))

   (step :Then "the number should be {long} and this step should fail"
         (fn [{:keys [actual] :as state} ^Long expected]
           (is (= expected actual))
           state))

   (step :Then "the number should satisfy multiple conditions"
         (fn [state]
           ;; Multiple assertions in one step - this is generally bad practice,
           ;; but useful for testing the failure reporting modes
           (is (= 42 (:number state)) "Number should be 42")
           (is (= 43 (:number state)) "Number should be 43") ; This failure will be reported
           (is (= 44 (:number state)) "Number should be 44") ; This failure will also be reported
           state))])

(deftest multiple-failures-are-not-reported-by-default
  (is (= 1 (run-cucumber "test-resources/features/clojure-test-assertion-macros.feature" steps))))

(deftest enabling-multiple-failures-all-mode
  ;; When *report-all-step-failures* is true, should report all failures
  (binding [runtime/*report-all-step-failures?* true]
    (is (= 1 (run-cucumber "test-resources/features/clojure-test-assertion-macros.feature" steps)))))
