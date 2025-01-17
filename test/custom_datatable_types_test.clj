(ns custom-datatable-types-test
  (:require [burpless :refer [datatable-type parameter-type run-cucumber step]]
            [clojure.test :refer [deftest is]])
  (:import (clojure.lang Keyword)
           (io.cucumber.datatable DataTable)
           (java.util List)))


;; FROM TABLE
(defrecord TicTacToeBoard [topLeft topMid topRight
                           midLeft midMid midRight
                           botLeft botMid botRight])


(def from-table-glues
  [(step :Given "I have the following tic-tac-toe board:"
         ^{:datatable TicTacToeBoard}
         (fn [state ^TicTacToeBoard board]
           (assoc state :board board)))

   (step :Then "the {keyword} square should be empty"
         (fn [{:keys [board] :as state} ^Keyword board-cell]
           (assert (= nil (board-cell board)))
           state))

   (step :Then "the {keyword} square should be {keyword}"
         (fn the_square_should_be [{:keys [board] :as state} ^Keyword board-cell ^Keyword expected-player]
           (let [actual-player (board-cell board)]
             (assert (= expected-player actual-player)))
           state))

   (datatable-type {:to-type   TicTacToeBoard
                    :from-type :table
                    :transform (fn [^DataTable table]
                                 (apply ->TicTacToeBoard (map keyword (.values table))))})])

(deftest from-table
  (is (zero? (run-cucumber "test-resources/features/custom-datatable-types/from-table.feature" from-table-glues))))


;; FROM ENTRY
(defrecord Book [^String title ^String author])

(def from-entry-glues
  [(step :When "I search for books by author {string}"
         (fn [state ^String author]
           (assoc state :matching-books (filter (comp #{author} :author) (:book-store state)))))

   (step :Then "I find {int} book(s)"
         (fn [state ^Integer num-books]
           (assert (= num-books (count (:matching-books state))))))

   (step :Given "I have the following books in the store"
         ^{:datatable [Book]}
         (fn [state ^List books]
           (assoc state :book-store books)))

   (datatable-type {:to-type   Book
                    :from-type :entry
                    :transform (fn [{:strs [title author]}]
                                 (->Book title author))})])

(deftest from-entry
  (is (zero? (run-cucumber "test-resources/features/custom-datatable-types/from-entry.feature" from-entry-glues))))


;; FROM CELL
(defrecord Color [name])

(def from-cell-glues
  [(parameter-type {:name      "color"
                    :regexps   [#"\w+"]
                    :to-type   Color
                    :transform ->Color})

   (step :Given "that my favorite colors are:"
         ^{:datatable [Color]}
         (fn [state ^List favorite-colors]
           (assoc state :favorite-colors favorite-colors)))

   (step :Then "{color} is one of my favorite colors"
         (fn [{:keys [favorite-colors] :as state} ^Color candidate]
           (assert (some? (some #{candidate} favorite-colors)))
           state))

   (step :Then "{color} is not one of my favorite colors"
         (fn [{:keys [favorite-colors] :as state} ^Color candidate]
           (assert (nil? (some #{candidate} favorite-colors)))
           state))

   (datatable-type {:to-type   Color
                    :from-type :cell
                    :transform ->Color})])

(deftest from-cell
  (is (zero? (run-cucumber "test-resources/features/custom-datatable-types/from-cell.feature" from-cell-glues))))


;; FROM ROW
(def from-row-glues
  [(step :Given "I have the following rows of test scores"
         ^{:datatable [List]}
         (fn [state ^List score-lists]
           (assoc state :score-lists score-lists)))

   (step :When "I calculate the averages of each row"
         (fn [{:keys [score-lists] :as state}]
           (assoc state :averages (mapv (fn [sl] (quot (apply + sl) (count sl)))
                                        score-lists))))

   (step :Then "my state should look like this:"
         (fn [actual-state ^String docString]
           (let [expected-state (read-string docString)]
             (is (= expected-state actual-state)))))

   (datatable-type {:to-type   List
                    :from-type :row
                    :transform (partial mapv parse-long)})])

(deftest from-row
  (is (zero? (run-cucumber "test-resources/features/custom-datatable-types/from-row.feature" from-row-glues))))
