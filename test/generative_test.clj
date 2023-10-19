(ns generative-test
  (:require [burpless :refer [run-cucumber]]
            [burpless.generative :as bg :refer [after-hook-with-cleanup before-hook generator property]]
            [clojure.test :refer [deftest is]]
            [clojure.test.check.generators :as gen]))

(def generative-cleanup-happened (atom false))
(def generative-steps
  [before-hook

   (generator :Given "any positive integer {word}"
              (fn gen-pos-int [_ var]
                [var (gen/fmap inc gen/nat)]))

   (generator :Given "any positive integer {word} greater than {word}"
              (fn gen-pos-int-gt [env var min-var]
                (let [min (get env min-var)]
                  [var (->> gen/nat
                            (gen/fmap (partial + min)))])))

   (generator :Given "any integer {word} from {long} to {long}"
              (fn gen-int-in-range [_env var lower upper]
                [var (gen/choose lower upper)]))

   (bg/step :When "a regular step happens"
            (fn regular-step [env]
              (assoc env :step-happened true)))

   (property "{word} + {word} is positive"
             (fn sum-is-positive [env var1 var2]
               (pos? (+ (get env var1)
                        (get env var2)))))

   (property "{word} + {word} is negative"
             (fn sum-is-negative [env var1 var2]
               (neg? (+ (get env var1)
                        (get env var2)))))

   (property "the regular step happened"
             (fn regular-step-happened [env]
               (:step-happened env)))

   (after-hook-with-cleanup
     (fn cleanup [_env]
       (reset! generative-cleanup-happened true)))])

(deftest generative-feature
  (is (zero? (run-cucumber "resources/features/generative.feature" generative-steps)))
  (is @generative-cleanup-happened))
