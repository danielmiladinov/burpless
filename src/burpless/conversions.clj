(ns burpless.conversions
  (:require [clojure.edn :as edn])
  (:import (io.cucumber.datatable DataTable)))

(defn read-cucumber-string
  "Using the clojure reader is often a good way to interpret literal values in feature files.
   This function makes some cucumber-specific adjustments to basic reader behavior.
   This is particularly appropriate when reading from a table, for example:
   reading | \"1\" | 1 | we should interpret 1 as an int and \"1\" as a string.

   However, when the result of reading the string produces a symbol instead of literal data,
   then we should return the input string unchanged."
  [^String cucumber-string]
  (when cucumber-string
    (let [result (edn/read-string cucumber-string)]
      (if (symbol? result)
        cucumber-string
        result))))

(defn key-value-table->map
  "Reads two-column table where each row represents a key-value pair of a map,
  returning the map with the keys as keywords.
   For example, given:
     | from | 15 |
     | to   | 25 |
   It evaluates to the clojure literal:
     {:from 15 :to 25}"
  [^DataTable dataTable]
  (-> (.asMap dataTable)
      (update-keys keyword)
      (update-vals read-cucumber-string)))

(defn data-table->maps
  "Reads a cucumber table wherein values may be EDN collections
     | key-1 | key-2 | ... | key-n |
     | val-1 | val-2 | ... | val-n |
     | ...                         |
     | row-n | ...   | ... | ...   |
   For example, given:
     | key                 | name    | created-at    |
     | {:id 55 :type :foo} | \"foo\" | 1293884100000 |
     | {:id 56 :type :bar} | \"bar\" | 1293884100001 |
   It evaluates to the clojure literal:
     [{:key {:id 55 :type :foo}, :name \"foo\", :created-at 1293884100000}
      {:key {:id 56 :type :bar}, :name \"bar\", :created-at 1293884100001}]"
  [^DataTable dataTable]
  (let [[keys & vals] (.asLists dataTable)]
    (mapv zipmap
          (repeat (map keyword keys))
          (map (partial map read-cucumber-string) vals))))
