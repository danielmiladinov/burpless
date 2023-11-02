Feature: Burpless Innately Supports EDN DocStrings

  Scenario: Populate State with DataTable inputs and compare against edn DocString input
    Given my state starts out as an empty map
    And I want to collect pairs of high and low temperatures under the :highs-and-lows state key
    When I have a table of the following high and low temperatures:
      | 81 | 49 |
      | 88 | 54 |
      | 76 | 56 |
      | 70 | 48 |
      | 81 | 55 |
    Then my state should be equal to the following Clojure edn literal:
    """edn
    {:target-keyword :highs-and-lows
     :highs-and-lows [[81 49] [88 54] [76 56] [70 48] [81 55]]}
    """

  Scenario: Converting Two-Column DataTables to Clojure maps
    Given I have a key-value table:
      | numeric-value       | 1                                                      |
      | bigint-value        | 7823N                                                  |
      | double-value        | 1.327                                                  |
      | big-decimal-value   | 5.32M                                                  |
      | true-value          | true                                                   |
      | false-value         | false                                                  |
      | set-value           | #{"foo" "bar" "baz"}                                   |
      | vector-value        | [:foo "bar" {:baz true}]                               |
      | ratio-value         | 22/7                                                   |
      | uuid-value          | #uuid"e5fa994f-a073-4f9b-a05e-c8c6f2eea11c"            |
      | string-value        | Multi-word strings don't need to be enclosed in quotes |
      | quoted-string-value | "But they can be, if you prefer"                       |
      | keyword-value       | :some-keyword                                          |
      | nil-value           |                                                        |
    Then my state should be equal to the following Clojure edn literal:
    """edn
      {:numeric-value       1
       :bigint-value        7823N
       :double-value        1.327
       :big-decimal-value   5.32M
       :true-value          true
       :false-value         false
       :set-value           #{"foo" "bar" "baz"}
       :vector-value        [:foo "bar" {:baz true}]
       :ratio-value         22/7
       :uuid-value          #uuid "e5fa994f-a073-4f9b-a05e-c8c6f2eea11c"
       :string-value        "Multi-word strings don't need to be enclosed in quotes"
       :quoted-string-value "But they can be, if you prefer"
       :keyword-value       :some-keyword
       :nil-value           nil}
     """

  Scenario: Converting N-Column DataTables to sequences of Clojure maps
    Given I have a table of data with key names in the first row:
      | id | first-name | middle-name | last-name | favorite-color |
      | 1  | Charles    | John        | Taylor    | red            |
      | 2  | Brian      |             | Jones     | blue           |
      | 3  | William    | David       | Miller    | green          |
      | 4  | Robert     |             | Smith     | black          |
    Then my state should be equal to the following Clojure edn literal:
    """edn
      [{:id 1 :first-name "Charles" :middle-name "John"  :last-name "Taylor" :favorite-color "red"}
       {:id 2 :first-name "Brian"   :middle-name nil     :last-name "Jones"  :favorite-color "blue"}
       {:id 3 :first-name "William" :middle-name "David" :last-name "Miller" :favorite-color "green"}
       {:id 4 :first-name "Robert"  :middle-name nil     :last-name "Smith"  :favorite-color "black"}]
    """
