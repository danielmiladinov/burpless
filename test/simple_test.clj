(ns simple-test
  (:require [burpless :refer [hook run-cucumber step]]
            [clojure.test :refer [deftest is]]))

(def after-hook-happened (atom false))

(def simple-feature-steps
  [(hook :before
         (constantly
           {:before-hook-happened true
            :before-step-count    0
            :after-step-count     0}))

   (hook :before-step
         (fn before-step-hook [state _]
           (update state :before-step-count inc)))

   (hook :after-step
         (fn after-step-hook [state _]
           (update state :after-step-count inc)))

   (hook :after
         (fn after-hook [state _]
           (reset! after-hook-happened true)
           state))

   (step :Given #"^some setup$"
         (fn some-setup [state]
           (assoc state :setup-happened true)))

   (step :When #"^I do a thing$"
         (fn I-do-a-thing [state]
           (assoc state :thing-happened true)))

   (step :Then #"^the setup happened$"
         (fn the-setup-happened [state]
           (assert (:setup-happened state))
           state))

   (step :Then #"^the before hook happened$"
         (fn the-before-hook-happened [state]
           (assert (:before-hook-happened state))
           state))

   (step :Then #"^the thing happened$"
         (fn the-thing-happened [state]
           (assert (:thing-happened state))
           state))

   (step :Then #"^the (\w+) step counter is (\d+)$"
         (fn the-step-counter-is [state kind val]
           (case kind
             "before" (assert (= val (:before-step-count state)))
             "after" (assert (= val (:after-step-count state))))
           state))])

(deftest simple-feature
  (is (zero? (run-cucumber "resources/features/simple.feature" simple-feature-steps))))
