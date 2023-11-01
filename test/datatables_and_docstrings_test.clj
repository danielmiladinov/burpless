(ns datatables-and-docstrings-test
  (:require [burpless :refer [run-cucumber step]]
            [burpless.conversions :as conversions]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]])
  (:import (io.cucumber.datatable DataTable)
           (io.cucumber.docstring DocString)
           (java.lang.reflect Type)))

(def datatables-and-docstrings-steps
  [(step :Given "my state starts out as an empty map"
         (constantly {}))

   (step :Given "I want to collect pairs of high and low temperatures under the {word} state key"
         (fn [state keyword-name]
           (assoc state :target-keyword (keyword (str/replace keyword-name #":" "")))))

   (step :When "I have a table of the following high and low temperatures:"
         ^:datatable
         (fn [state ^DataTable dataTable]
           (assoc state :highs-and-lows (.asLists dataTable ^Type Long))))

   (step :Given "I have a key-value table:"
         ^:datatable
         (fn [_state ^DataTable dataTable]
           (conversions/key-value-table->map dataTable)))


   (step :Given "I have a table of data with key names in the first row:"
         ^:datatable
         (fn [_state ^DataTable dataTable]
           (conversions/data-table->maps dataTable)))

   (step :Then "my state should be equal to the following Clojure literal:"
         ^:docstring
         (fn [actual-state ^DocString docString]
           (let [expected-state (conversions/read-cucumber-string (.getContent docString))]
             (assert (= expected-state actual-state)))))])

(deftest datatable-and-docstrings
  (is (zero? (run-cucumber "resources/features/datatables-and-docstrings.feature" datatables-and-docstrings-steps))))
