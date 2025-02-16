(ns custom-parameter-types-test
  (:require [burpless :refer [run-cucumber step parameter-type]]
            [clojure.test :refer [deftest is]])
  (:import (clojure.lang IObj Keyword PersistentHashMap)))

(def glues
  [(parameter-type {:name      "bar-map"
                    :regexps   [#"bar-map of (\d+)"]
                    :to-type   PersistentHashMap
                    :transform (fn [^String num-string]
                                 (hash-map :bar (Long/valueOf num-string)))})

   (step :Given "my state starts out as an empty map"
         (constantly {}))

   (step :Given "burpless innately supports keyword parameter types" identity)

   (step :Given "I want to add a {bar-map} under the {keyword} key of my state"
         (fn [state ^PersistentHashMap bar-map ^Keyword kw]
           (assoc state kw bar-map)))

   (step :Then "my state should look like this"
         (fn [actual-state ^IObj expected-state]
           (is (= expected-state actual-state))))])

(deftest custom-parameter-types
  (is (zero? (run-cucumber "test-resources/features/custom-parameter-types.feature" glues))))
