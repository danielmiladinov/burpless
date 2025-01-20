(ns custom-docstring-types-test
  (:require [burpless :refer [docstring-type parameter-type run-cucumber step]]
            [burpless.conversions :as conversions]
            [camel-snake-kebab.core :as csk]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]])
  (:import (clojure.lang IObj Keyword)
           (io.cucumber.datatable DataTable)
           (java.lang.reflect Type)))

(def from-edn-steps
  [(step :Given "my state starts out as an empty map"
         (constantly {}))

   (step :Given "I want to collect pairs of high and low temperatures under the {word} state key"
         (fn [state keyword-name]
           (assoc state :target-keyword (keyword (str/replace keyword-name #":" "")))))

   (step :When "I have a table of the following high and low temperatures:"
         (fn [state ^DataTable dataTable]
           (assoc state :highs-and-lows (.asLists dataTable ^Type Long))))

   (step :Given "I have a key-value table:"
         (fn [_state ^DataTable dataTable]
           (conversions/key-value-table->map dataTable)))

   (step :Given "I have a table of data with key names in the first row:"
         (fn [_state ^DataTable dataTable]
           (conversions/data-table->maps dataTable)))

   (step :Then "my state should be equal to the following Clojure edn literal:"
         (fn [actual-state ^IObj expected-state]
           (is (= expected-state actual-state))))])

(deftest from-edn
  (is (zero? (run-cucumber "test-resources/features/custom-docstring-types/from-edn.feature" from-edn-steps))))


(def from-json-steps
  [(step :Given "that my state starts out as an empty map"
         (constantly {}))

   (step :Given "I want to store the following in my state's {keyword} key"
         (fn [state ^Keyword kw ^IObj data]
           (assoc state kw data)))

   (step :When "I compare the my state's {keyword} and {keyword} keys to each other for equality, storing the result in {keyword}"
         (fn [state ^Keyword kw-1 ^Keyword kw-2 ^Keyword kw-3]
           (assoc state kw-3 (= (kw-1 state) (kw-2 state)))))

   (step :Then "my state's {keyword} equality value should be {boolean}"
         (fn [state ^Keyword kw ^Boolean equal?]
           (assert equal? (kw state))
           state))

   (docstring-type {:content-type "json"
                    :to-type      IObj
                    :transform    (fn [s] (json/read-str s :key-fn csk/->kebab-case-keyword))})

   (parameter-type {:name      "boolean"
                    :regexps   [#"(?i)true|false"]
                    :to-type   Boolean
                    :transform (fn [s] (Boolean/valueOf ^String s))})])

(deftest from-json
  (is (zero? (run-cucumber "test-resources/features/custom-docstring-types/from-json.feature" from-json-steps))))
