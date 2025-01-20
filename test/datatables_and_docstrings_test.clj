(ns datatables-and-docstrings-test
  (:require [burpless :refer [run-cucumber step]]
            [burpless.conversions :as conversions]
            [clojure.test :refer [deftest is]])
  (:import (clojure.lang IObj Keyword)
           (io.cucumber.datatable DataTable)
           (java.lang.reflect Type)))

(def datatables-and-docstrings-steps
  [(step :Given "my state starts out as an empty map"
         (constantly {}))

   (step :Given "I want to collect pairs of high and low temperatures under the {keyword} state key"
         (fn [state ^Keyword kw]
           (assoc state :target-keyword kw)))

   (step :When "I have a table of the following high and low temperatures:"
         (fn [state ^DataTable dataTable]
           (assoc state :highs-and-lows (.asLists dataTable ^Type Long))))

   (step :Given "I have a key-value table:"
         (fn [_state ^DataTable dataTable]
           (conversions/key-value-table->map dataTable)))


   (step :Given "I have a table of data with key names in the first row:"
         (fn [_state ^DataTable dataTable]
           (conversions/data-table->maps dataTable)))

   (step :Then "my state should be equal to the following Clojure literal:"
         (fn [actual-state ^IObj expected-state]
           (assert (= expected-state actual-state))))])

(deftest datatable-and-docstrings
  (is (zero? (run-cucumber "test-resources/features/datatables-and-docstrings.feature" datatables-and-docstrings-steps))))
