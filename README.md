# danielmiladinov/burpless

An idiomatic Clojure wrapper for the latest version of [cucumber-jvm](https://github.com/cucumber/cucumber-jvm),
inspired by [auxoncorp/clj-cucumber](https://github.com/auxoncorp/clj-cucumber).

Library name suggested by [Roman Ostash](https://github.com/Romacoding).

- [Usage](#usage)
  - [Add the Dependency](#add-the-dependency)
  - [Write a Feature File](#write-a-feature-file)
  - [Write a Test File](#write-a-test-file)
  - [Run the tests and copy the step definition snippets from the output](#run-the-tests-and-copy-the-step-definition-snippets-from-the-output)
  - [Copy and Paste the Step Functions](#copy-and-paste-the-step-functions)
  - [Burpless Step Functions](#burpless-step-functions)
    - [Doc Strings and Data Tables](#doc-strings-and-data-tables)
  - [Update the Step Functions to Pass the Test](#update-the-step-functions-to-pass-the-test)
- [License](#license)

## Usage

### Add the Dependency
Add it to your `deps.edn`:
```clojure
{:deps {danielmiladinov/burpless {:git/url "https://github.com/danielmiladinov/burpless"
                                  :git/tag "0.0.1"
                                  :git/sha "064ae51771f0696dc7d4adb13890d13437b592df"}}}
```

### Write a Feature File

Save the following as `test/my-first.feature`:
```gherkin
Feature: My first feature

  Scenario: Learning to use Burpless
    Given I have a string value of "Hello, Burpless!" under the :message key in my state
    And I have a long value of 5 under the :stars key in my state
    And I have a table of the following high and low temperatures:
      | 81 | 49 |
      | 88 | 54 |
      | 76 | 56 |
      | 70 | 48 |
      | 81 | 55 |
    When I am ready to check my state
    Then my state should be equal to the following Clojure literal:
    """
    {:message "Hello, Burpless!"
     :stars 5
     :highs-and-lows [[81 49] [88 54] [76 56] [70 48] [81 55]]
     :ready-to-check? true}
    """
```
Yes, burpless supports `DataTable` and `DocString` step arguments! More on that later.

### Write a Test File
Save the following as `test/my-first-feature-test.clj`.
For now, it's relatively empty, but we'll be adding more to it shortly:
```clojure
(ns my-first-feature-test
  (:require [clojure.test :refer [deftest is]]
            [burpless :refer [run-cucumber step]]))

(def steps
  [])

(deftest my-first-feature
  (is (zero? (run-cucumber "test/my-first.feature" steps))))
```

### Run the tests and copy the step definition snippets from the output
Run the test using your preferred test runner. Below is just one possible method:
```bash
$ clojure -T:build test
```

You should see output similar to the following:
```
Running tests in #{"test"}

Testing my-first-feature-test

Scenario: Learning to use Burpless                                                     # test/my-first.feature:3
  Given I have a string value of "Hello, Burpless!" under the :message key in my state
  And I have a long value of 5 under the :stars key in my state
  And I have a table of the following high and low temperatures:
    | 81 | 49 |
    | 88 | 54 |
    | 76 | 56 |
    | 70 | 48 |
    | 81 | 55 |
  When I am ready to check my state
  Then my state should be equal to the following Clojure literal:

Undefined scenarios:
file:///path/to/my-first.feature:3 # Learning to use Burpless

1 Scenarios (1 undefined)
5 Steps (4 skipped, 1 undefined)
0m0.041s


You can implement missing steps with the snippets below:

(step :Given "I have a string value of {string} under the :message key in my state"
      (fn i_have_a_string_value_of_under_the_message_key_in_my_state [state ^String string]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Given "I have a long value of {int} under the :stars key in my state"
      (fn i_have_a_long_value_of_under_the_stars_key_in_my_state [state ^Integer int1]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Given "I have a table of the following high and low temperatures:"
      (fn i_have_a_table_of_the_following_high_and_low_temperatures [state ^io.cucumber.datatable.DataTable dataTable]
        ;; Write code here that turns the phrase above into concrete actions
        ;; Be sure to also adorn your step function with the ^:datatable metadata
        ;; in order for the runtime to properly identify it and pass the datatable argument
        (throw (io.cucumber.java.PendingException.))))

(step :When "I am ready to check my state"
      (fn i_am_ready_to_check_my_state [state ]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Then "my state should be equal to the following Clojure literal:"
      (fn my_state_should_be_equal_to_the_following_clojure_literal [state ^String docString]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))



FAIL in (my-first-feature) (my_first_feature_test.clj:35)
expected: (zero? (run-cucumber "test/my-first.feature" steps))
  actual: (not (zero? 1))

Ran 1 tests containing 1 assertions.
1 failures, 0 errors.
Execution error (ExceptionInfo) at build/test (build.clj:19).
Tests failed

Full report at:
/path/to/full-report.edn
```

### Copy and Paste the Step Functions
While these step functions in their current form definitely will not make the feature pass,
they will at least give us a good starting-off point to build towards a possible solution.
```clojure
(step :Given "I have a string value of {string} under the :message key in my state"
      (fn i_have_a_string_value_of_under_the_message_key_in_my_state [state ^String string]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Given "I have a long value of {int} under the :stars key in my state"
      (fn i_have_a_long_value_of_under_the_stars_key_in_my_state [state ^Integer int1]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Given "I have a table of the following high and low temperatures:"
      (fn i_have_a_table_of_the_following_high_and_low_temperatures [state ^io.cucumber.datatable.DataTable dataTable]
        ;; Write code here that turns the phrase above into concrete actions
        ;; Be sure to also adorn your step function with the ^:datatable metadata
        ;; in order for the runtime to properly identify it and pass the datatable argument
        (throw (io.cucumber.java.PendingException.))))

(step :When "I am ready to check my state"
      (fn i_am_ready_to_check_my_state [state ]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Then "my state should be equal to the following Clojure literal:"
      (fn my_state_should_be_equal_to_the_following_clojure_literal [state ^String docString]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))
```
Pay attention to the comments embedded in the step functions; if you are following closely, you'll see some things that
may not immediately make sense:
- Why does each step function have a first argument called `state`?
- What is this `^io.cucumber.datatable.DataTable dataTable` argument all about?
- What does this comment mean?
```clojure
 ;; Be sure to also adorn your step function with the ^:datatable metadata
 ;; in order for the runtime to properly identify it and pass the datatable argument
```
Let's try to answer these questions in the next section.

### Burpless Step Functions
For every call to `run-cucumber`, Burpless instantiates a Cucumber Backend as well as a Cucumber runtime, with which
to test your feature. To keep things simple, it expects to run only a single feature at a time. For your convenience,
`run-cucumber` also maintains an [`atom`](https://clojure.org/reference/atoms) to hold all of the state against which
your step functions will be executed.

We use the burpless macro, `step`, to define our step functions. It takes three parameters:
- A Clojure keyword representing one of the [Gherkin keywords](https://cucumber.io/docs/gherkin/reference/#keywords)
- A string representing either a [CucumberExpression](https://github.com/cucumber/cucumber-expressions#readme) (preferred) or
[RegularExpression](https://en.wikipedia.org/wiki/Regular_expression) pattern to match for the step
  - (all regular expression patterns must start with `^` and end with `$` or they will be interpreted as
    cucumber expressions by the Cucumber runtime.)
- The function to call when executing the step. Every step function will receive the current value of the state atom
as its first argument. Any output parameters (`CucumberExpression`) or capture groups (`RegularExpression`) matched in
the pattern are provided as additional arguments to the function.

#### Doc Strings and Data Tables

Burpless' implementation of the Cucumber [`Backend`](https://github.com/cucumber/cucumber-jvm/blob/c82382e31d7b4c8ad2292a24cfbc153625b4d343/cucumber-core/src/main/java/io/cucumber/core/backend/Backend.java) interface
is responsible for adding [`StepDefinition`](https://github.com/cucumber/cucumber-jvm/blob/c82382e31d7b4c8ad2292a24cfbc153625b4d343/cucumber-core/src/main/java/io/cucumber/core/backend/StepDefinition.java)s to the [`Glue`](https://github.com/cucumber/cucumber-jvm/blob/c82382e31d7b4c8ad2292a24cfbc153625b4d343/cucumber-core/src/main/java/io/cucumber/core/backend/Glue.java) instance
provided to it during the call to [`loadGlue()`](https://github.com/cucumber/cucumber-jvm/blob/c82382e31d7b4c8ad2292a24cfbc153625b4d343/cucumber-core/src/main/java/io/cucumber/core/backend/Backend.java#L18),
and they must return [`ParameterInfo`](https://github.com/cucumber/cucumber-jvm/blob/c82382e31d7b4c8ad2292a24cfbc153625b4d343/cucumber-core/src/main/java/io/cucumber/core/backend/ParameterInfo.java) [lists](https://github.com/cucumber/cucumber-jvm/blob/c82382e31d7b4c8ad2292a24cfbc153625b4d343/cucumber-core/src/main/java/io/cucumber/core/backend/StepDefinition.java#L24)
that match what the Cucumber runtime discovered while parsing the feature file(s) into Gherkin steps, or else Cucumber
will report that step as undefined.

The current design of the Cucumber JVM library tries to make it very easy to identify the code that should run for a
particular Gherkin step - assuming that your JVM language is strongly typed, and has excellent annotation support.
Just [annotate your methods with the appropriate annotation(s)](https://cucumber.io/docs/cucumber/step-definitions/?lang=java),
and the cucumber runtime does the rest!

Coming from Clojure, that's two strikes against us.

Using Cucumber Expressions, it's fairly easy to extract output parameter info from the pattern itself, but that doesn't
help us with `DataTable` or `DocString` parameters. They aren't part of the pattern, but follow in the next line(s) in
the feature file. While there are ways to
[reflectively get the type hints on Clojure function parameters](https://www.google.com/search?q=clojure+function+reflection+get+type+hint),
these only seem to work for top-level functions defined with `defn`, not for the inline functions defined with `fn` and
passed to the `step` macro. The parameter info for each `StepDefinition` has to come from somewhere, as [`parameterInfos()` method](https://github.com/cucumber/cucumber-jvm/blob/c82382e31d7b4c8ad2292a24cfbc153625b4d343/cucumber-core/src/main/java/io/cucumber/core/backend/StepDefinition.java#L24)
takes no arguments.

If there were another way to make this easier to do in Clojure, I would do it. But since I haven't yet found it, or it's
not possible, then, for step functions intended to match steps that are following by a [`DataTable`](https://github.com/cucumber/cucumber-jvm/blob/c82382e31d7b4c8ad2292a24cfbc153625b4d343/datatable/src/main/java/io/cucumber/datatable/DataTable.java)
then you must tag your step function with the `^:datatable` metadata:
```clojure
(step :Given "I want to receive a DataTable parameter to my step function"
      ^:datatable
      (fn [state ^io.cucumber.datatable.DataTable dataTable]
        ;; Do something interesting with state and dataTable, returning an updated state
        ))
```

Similarly, for step functions intended to match steps that are followed by a [`DocString`](https://github.com/cucumber/cucumber-jvm/blob/c82382e31d7b4c8ad2292a24cfbc153625b4d343/docstring/src/main/java/io/cucumber/docstring/DocString.java),
you must tag your step function with the `^:docstring` metadata:
```clojure
(step :Given "I want to receive a DocString parameter to my step function"
      ^:docstring
      (fn [state ^io.cucumber.docstring.DocString docString]
        ;; Do something interesting with the state and docString, returning an updated state
        ))

```

### Update the Step Functions to Pass the Test
While you might be able to come up with something slightly different, here's one possible implementation of step functions
that makes the test pass:
```clojure
(ns my-first-feature-test
  (:require [burpless :refer [run-cucumber step]]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]])
  (:import (io.cucumber.datatable DataTable)
           (io.cucumber.docstring DocString)
           (java.lang.reflect Type)))

(def steps
  [(step :Given "I have a string value of {string} under the :message key in my state"
         (fn [state ^String message]
           (assoc state :message message)))

   (step :Given "I have a long value of {long} under the {word} key in my state"
         (fn [state ^Long stars ^String keyword-name]
           (assoc state (keyword (str/replace keyword-name #":" "")) stars)))

   (step :Given "I have a table of the following high and low temperatures:"
         ^:datatable
         (fn [state ^DataTable dataTable]
           (assoc state :highs-and-lows (.asLists dataTable ^Type Long))))

   (step :When "I am ready to check my state"
         (fn [state]
           (assoc state :ready-to-check? true)))

   (step :Then "my state should be equal to the following Clojure literal:"
         ^:docstring
         (fn [actual-state ^DocString docString]
           (let [expected-state (read-string (.getContent docString))]
             (is (= expected-state actual-state)))))])

(deftest my-first-feature
  (is (= 0 (run-cucumber "test/my-first.feature" steps))))
```

Run the tests again:
```
$ clojure -T:build test

Running tests in #{"test"}

Testing my-first-feature-test

Scenario: Learning to use Burpless                                                     # test/my-first.feature:3
  Given I have a string value of "Hello, Burpless!" under the :message key in my state # my_first_feature_test.clj:10
  And I have a long value of 5 under the :stars key in my state                        # my_first_feature_test.clj:14
  And I have a table of the following high and low temperatures:                       # my_first_feature_test.clj:18
    | 81 | 49 |
    | 88 | 54 |
    | 76 | 56 |
    | 70 | 48 |
    | 81 | 55 |
  When I am ready to check my state                                                    # my_first_feature_test.clj:23
  Then my state should be equal to the following Clojure literal:                      # my_first_feature_test.clj:27

1 Scenarios (1 passed)
5 Steps (5 passed)
0m0.037s



Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
```

Happy Cucumbering!


## License

[![License](https://img.shields.io/badge/License-Apache_2.0-yellowgreen.svg)](https://opensource.org/licenses/Apache-2.0)

    Copyright 2023 Daniel Miladinov

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
