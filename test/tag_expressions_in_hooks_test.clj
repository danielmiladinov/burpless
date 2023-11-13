(ns tag-expressions-in-hooks-test
  (:require [burpless :refer [hook run-cucumber step]]
            [camel-snake-kebab.core :as csk]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(def tag-expressions-in-hooks-glues
  [(hook :before-all
         (constantly {:first-scenario   {:glues-ran []}
                      :second-scenario  {:glues-ran []}
                      :third-scenario   {:glues-ran []}
                      :current-scenario nil}))

   (hook :before
         (fn [state testCaseState]
           (let [current-scenario (-> (.getName testCaseState)
                                      str/lower-case
                                      csk/->kebab-case-keyword)]
             (update-in state [current-scenario :glues-ran] conj {:type  :hook
                                                                  :phase :before}))))

   (hook :before
         ^{:tag "@First or @Second"}
         (fn [state testCaseState]
           (let [current-scenario (-> (.getName testCaseState)
                                      str/lower-case
                                      csk/->kebab-case-keyword)]
             (update-in state [current-scenario :glues-ran] conj {:type  :hook
                                                                  :phase :before
                                                                  :tag   "@First or @Second"}))))

   (hook :before
         ^{:tag "not @First"}
         (fn [state testCaseState]
           (let [current-scenario (-> (.getName testCaseState)
                                      str/lower-case
                                      csk/->kebab-case-keyword)]
             (update-in state [current-scenario :glues-ran] conj {:type  :hook
                                                                  :phase :before
                                                                  :tag   "not @First"}))))

   (hook :before
         ^{:tag "@Second or @Third"}
         (fn [state testCaseState]
           (let [current-scenario (-> (.getName testCaseState)
                                      str/lower-case
                                      csk/->kebab-case-keyword)]
             (update-in state [current-scenario :glues-ran] conj {:type  :hook
                                                                  :phase :before
                                                                  :tag   "@Second or @Third"}))))

   (hook :before-step
         (fn [state testCaseState]
           (let [current-scenario (-> (.getName testCaseState)
                                      str/lower-case
                                      csk/->kebab-case-keyword)]
             (-> (update-in state [current-scenario :glues-ran] conj {:type  :hook
                                                                      :phase :before-step})
                 (assoc :current-scenario current-scenario)))))

   (step :Given "this is the {word} step"
         (fn [{:keys [current-scenario] :as state} ^String ordinal]
           (update-in state [current-scenario :glues-ran] conj {:type    :step
                                                                :ordinal ordinal})))

   (hook :after-step
         (fn [state testCaseState]
           (let [current-scenario (-> (.getName testCaseState)
                                      str/lower-case
                                      csk/->kebab-case-keyword)]
             (-> (update-in state [current-scenario :glues-ran] conj {:type  :hook
                                                                      :phase :after-step})
                 (dissoc :current-scenario)))))

   (hook :after
         (fn [state testCaseState]
           (let [current-scenario (-> (.getName testCaseState)
                                      str/lower-case
                                      csk/->kebab-case-keyword)]
             (update-in state [current-scenario :glues-ran] conj {:type  :hook
                                                                  :phase :after}))))

   (hook :after
         ^{:tag "@First or @Third"}
         (fn [state testCaseState]
           (let [current-scenario (-> (.getName testCaseState)
                                      str/lower-case
                                      csk/->kebab-case-keyword)]
             (update-in state [current-scenario :glues-ran] conj {:type  :hook
                                                                  :phase :after
                                                                  :tag   "@First or @Third"}))))

   (hook :after
         ^{:tag "@Second"}
         (fn [state testCaseState]
           (let [current-scenario (-> (.getName testCaseState)
                                      str/lower-case
                                      csk/->kebab-case-keyword)]
             (update-in state [current-scenario :glues-ran] conj {:type  :hook
                                                                  :phase :after
                                                                  :tag   "@Second"}))))

   (hook :after
         ^{:tag "not @First"}
         (fn [state testCaseState]
           (let [current-scenario (-> (.getName testCaseState)
                                      str/lower-case
                                      csk/->kebab-case-keyword)]
             (update-in state [current-scenario :glues-ran] conj {:type  :hook
                                                                  :phase :after
                                                                  :tag   "not @First"}))))

   (hook :after-all
         (fn [actual-state]
           (let [expected-state {:first-scenario  {:glues-ran [{:type :hook :phase :before}
                                                               {:type :hook :phase :before :tag "@First or @Second"}

                                                               {:type :hook :phase :before-step}
                                                               {:type :step :ordinal "first"}
                                                               {:phase :after-step :type :hook}

                                                               {:type :hook :phase :before-step}
                                                               {:type :step :ordinal "second"}
                                                               {:phase :after-step :type :hook}

                                                               {:type :hook :phase :before-step}
                                                               {:type :step :ordinal "third"}
                                                               {:phase :after-step :type :hook}

                                                               {:type :hook :phase :after :tag "@First or @Third"}
                                                               {:type :hook :phase :after}]}

                                 :second-scenario {:glues-ran [{:type :hook :phase :before}
                                                               {:type :hook :phase :before :tag "@First or @Second"}
                                                               {:type :hook :phase :before :tag "not @First"}
                                                               {:type :hook :phase :before :tag "@Second or @Third"}

                                                               {:type :hook :phase :before-step}
                                                               {:type :step :ordinal "first"}
                                                               {:phase :after-step :type :hook}

                                                               {:type :hook :phase :before-step}
                                                               {:type :step :ordinal "second"}
                                                               {:phase :after-step :type :hook}

                                                               {:type :hook :phase :before-step}
                                                               {:type :step :ordinal "third"}
                                                               {:phase :after-step :type :hook}

                                                               {:type :hook :phase :after :tag "not @First"}
                                                               {:type :hook :phase :after :tag "@Second"}
                                                               {:type :hook :phase :after}]}

                                 :third-scenario  {:glues-ran [{:type :hook :phase :before}
                                                               {:type :hook :phase :before :tag "not @First"}
                                                               {:type :hook :phase :before :tag "@Second or @Third"}

                                                               {:type :hook :phase :before-step}
                                                               {:type :step :ordinal "first"}
                                                               {:phase :after-step :type :hook}

                                                               {:type :hook :phase :before-step}
                                                               {:type :step :ordinal "second"}
                                                               {:phase :after-step :type :hook}

                                                               {:type :hook :phase :before-step}
                                                               {:type :step :ordinal "third"}
                                                               {:phase :after-step :type :hook}

                                                               {:type :hook :phase :after :tag "not @First"}
                                                               {:type :hook :phase :after :tag "@First or @Third"}
                                                               {:type :hook :phase :after}]}}]
             (assert (= expected-state actual-state)))))])

(deftest tag-expressions-in-hooks-test
  (is (zero? (run-cucumber "test-resources/features/tag-expressions-in-hooks.feature" tag-expressions-in-hooks-glues))))
