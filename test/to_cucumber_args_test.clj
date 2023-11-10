(ns to-cucumber-args-test
  (:require [burpless :refer [run-cucumber step docstring-type]]
            [burpless.runtime :refer [to-cucumber-args]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]])
  (:import (clojure.lang IObj Keyword)
           (io.cucumber.docstring DocString)))

(def to-cucumber-args-steps
  [(step :Given "the args map begins as an empty map"
         (constantly {}))

   (step :When "the {keyword} arg is set to {string}"
         (fn [state ^Keyword arg-name ^String arg-value]
           (assoc state arg-name arg-value)))

   (step :When "the args are set to the following edn:"
         ^{:docstring IObj}
         (fn [_state ^IObj args]
           args))

   (step :When "the {keyword} arg is set to nil"
         (fn [state ^Keyword arg-name]
           (assoc state arg-name nil)))

   (step :Then "the args output should be equal to {string}"
         (fn [state ^String expected-output]
           (let [actual-output (->> state to-cucumber-args (str/join " "))]
             (is (= expected-output actual-output)))))

   (docstring-type {:content-type "multiline-trimmed-string"
                    :to-type      String
                    :transform    (fn [^String ds]
                                    (->> ds
                                         (str/split-lines)
                                         (map str/trim)
                                         (str/join " ")))})

   (step :Then "the args output should be equal to"
         ^{:docstring String}
         (fn [state ^String expected-output]
           (let [actual-output (->> state to-cucumber-args (str/join " "))]
             (is (= expected-output actual-output)))))])

(deftest to-cucumber-args-test
  (is (zero? (run-cucumber "test-resources/features/to-cucumber-args.feature" to-cucumber-args-steps))))
