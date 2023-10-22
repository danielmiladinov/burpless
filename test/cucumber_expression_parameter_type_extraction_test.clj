(ns cucumber-expression-parameter-type-extraction-test
  (:require [burpless :refer [run-cucumber step]]
            [clojure.test :refer [deftest is]]))

(def steps
  [(step :When "I have an int value of {int}"
         (fn [state ^Integer value]
           (assoc state :parameter-value value)))

   (step :Then "its int value should be {int}"
         (fn [{:keys [parameter-value] :as state} ^Integer expected]
           (assert (= expected parameter-value))
           state))

   (step :When "I have a float value of {float}"
         (fn [state ^Float value]
           (assoc state :parameter-value value)))

   (step :Then "its float value should be {float}"
         (fn [{:keys [parameter-value] :as state} ^Float expected]
           (assert (= expected parameter-value))
           state))

   (step :When "I have a word value of {word}"
         (fn [state ^String value]
           (assoc state :parameter-value value)))

   (step :Then "its word value should be {word}"
         (fn [{:keys [parameter-value] :as state} ^String expected]
           (assert (= expected parameter-value))
           state))

   (step :When "I have a string value of {string}"
         (fn [state ^String value]
           (assoc state :parameter-value value)))

   (step :Then "its string value should be {string}"
         (fn [{:keys [parameter-value] :as state} ^String expected]
           (assert (= expected parameter-value))
           state))

   (step :When "I have an anonymous value of {}"
         (fn [state ^String value]
           (assoc state :parameter-value value)))

   (step :Then "its anonymous value should be {}"
         (fn [{:keys [parameter-value] :as state} ^String expected]
           (assert (= expected parameter-value))
           state))

   (step :When "I have a bigdecimal value of {bigdecimal}"
         (fn [state ^BigDecimal value]
           (assoc state :parameter-value value)))

   (step :Then "its bigdecimal value should be {bigdecimal}"
         (fn [{:keys [parameter-value] :as state} ^BigDecimal expected]
           (assert (= expected parameter-value))
           state))

   (step :When "I have a double value of {double}"
         (fn [state ^Double value]
           (assoc state :parameter-value value)))

   (step :Then "its double value should be {double}"
         (fn [{:keys [parameter-value] :as state} ^Double expected]
           (assert (= expected parameter-value))
           state))

   (step :When "I have a biginteger value of {biginteger}"
         (fn [state ^BigInteger value]
           (assoc state :parameter-value value)))

   (step :Then "its biginteger value should be {biginteger}"
         (fn [{:keys [parameter-value] :as state} ^BigInteger expected]
           (assert (= expected parameter-value))
           state))

   (step :When "I have a byte value of {byte}"
         (fn [state ^Byte value]
           (assoc state :parameter-value value)))

   (step :Then "its byte value should be {byte}"
         (fn [{:keys [parameter-value] :as state} ^Byte expected]
           (assert (= expected parameter-value))
           state))

   (step :When "I have a short value of {short}"
         (fn [state ^Short value]
           (assoc state :parameter-value value)))

   (step :Then "its short value should be {short}"
         (fn [{:keys [parameter-value] :as state} ^Short expected]
           (assert (= expected parameter-value))
           state))

   (step :When "I have a long value of {long}"
         (fn [state ^Long value]
           (assoc state :parameter-value value)))

   (step :Then "its long value should be {long}"
         (fn [{:keys [parameter-value] :as state} ^Long expected]
           (assert (= expected parameter-value))
           state))

   (step :And "its type should be {}"
         (fn [{:keys [parameter-value] :as state} ^String type-name]
           (assert (= (Class/forName type-name)
                      (type parameter-value)))
           state))])

(deftest using-cucumber-expressions
  (is (zero? (run-cucumber "resources/features/cucumber-expression-parameter-type-extraction.feature" steps))))
