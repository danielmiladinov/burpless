(ns test-around-hooks
  (:require [clojure.test :refer [deftest is testing]]
            [burpless :refer [run-cucumber step hook]]
            [burpless.conversions :as bc])
  (:import (io.cucumber.datatable DataTable)))

;; Test around hooks functionality
(def execution-log (atom []))

(defn log [msg]
  (swap! execution-log conj msg))

(def steps
  [(step :Given "I have a simple step"
         (fn [state]
           (log :step-executed)
           (assoc state :simple-step-executed true)))

   (step :And "the number is {int}"
         (fn [state n]
           (log :param-step)
           (assoc state :param-step-number n)))

   (step :Given "the following users:"
         (fn [state ^DataTable data-table]
           (log :datatable-step)
           (assoc state :datatable-step-users (bc/data-table->maps data-table))))

   (step :Given "the following docstring:"
         (fn [state docstring]
           (log :docstring-step)
           (assoc state :docstring-step-value docstring)))])


(deftest test-around-step-hooks
  (testing "around-step hooks wrap individual step execution (with params, DataTables + DocStrings)"
    (reset! execution-log [])
    (let [result (run-cucumber "test-resources/features/around-hooks.feature"
                               (concat steps
                                       [(hook :around-step
                                              (fn [state run-step]
                                                (log :around-before)
                                                (let [result (run-step state)]
                                                  (log :around-after)
                                                  result)))
                                        (hook :after-all
                                              (fn [state]
                                                (is (= {:simple-step-executed true
                                                        :param-step-number    42
                                                        :docstring-step-value "Hello from DocString"
                                                        :datatable-step-users [{:name "Alice" :age 30}
                                                                               {:name "Bob" :age 25}]}
                                                       state))))]))]
      (is (= [:around-before :step-executed :around-after
              :around-before :step-executed :around-after
              :around-before :param-step :around-after
              :around-before :datatable-step :around-after
              :around-before :docstring-step :around-after] @execution-log))
      (is (zero? result)))))

(deftest test-multiple-around-hooks
  (testing "multiple around hooks compose correctly (with params, DataTables + DocStrings)"
    (reset! execution-log [])
    (let [result (run-cucumber "test-resources/features/around-hooks.feature"
                               (conj steps
                                     (hook :around-step (fn [state run-step] (log :hook1-before) (let [s (run-step state)] (log :hook1-after) s)))
                                     (hook :around-step (fn [state run-step] (log :hook2-before) (let [s (run-step state)] (log :hook2-after) s)))))]
      ;; hook1 outermost; runs for all 4 steps across 3 scenarios
      (is (= [:hook1-before :hook2-before :step-executed :hook2-after :hook1-after
              :hook1-before :hook2-before :step-executed :hook2-after :hook1-after
              :hook1-before :hook2-before :param-step :hook2-after :hook1-after
              :hook1-before :hook2-before :datatable-step :hook2-after :hook1-after
              :hook1-before :hook2-before :docstring-step :hook2-after :hook1-after] @execution-log))
      (is (zero? result)))))
