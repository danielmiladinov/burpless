(ns scenario-outline-test
  (:require [burpless :refer [run-cucumber step]]
            [clojure.test :refer [deftest is]]))

(defn is-friday-yet? [{:keys [today]}]
  (case today
    ("Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Saturday") "Nope"
    "Friday" "TGIF!"
    "That's not a day I recognize"))

(def scenario-outline-steps
  [(step :Given "today is {string}"
         (fn [state ^String today]
           (assoc state :today today)))

   (step :When "I ask whether it's Friday yet"
         (fn [state]
           (assoc state :actual-answer (is-friday-yet? state))))


   (step :Then "I should be told {string}"
         (fn [{:keys [actual-answer] :as state} ^String expected-answer]
           (assert (= expected-answer actual-answer))
           state))])

(deftest scenario-outline
  (is (zero? (run-cucumber "test-resources/features/scenario-outline.feature" scenario-outline-steps))))
