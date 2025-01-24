# danielmiladinov/burpless

An idiomatic Clojure wrapper around [cucumber-jvm](https://github.com/cucumber/cucumber-jvm), for writing Cucumber feature tests.

Library name inspired by [Roman Ostash](https://github.com/Romacoding).

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.danielmiladinov/burpless.svg)](https://clojars.org/net.clojars.danielmiladinov/burpless)

- [Usage](#usage)
  - [Add the Dependency](#add-the-dependency)
  - [Write a Feature File](#write-a-feature-file)
  - [Write a Test File](#write-a-test-file)
  - [Run the tests and copy the step definition snippets from the output](#run-the-tests-and-copy-the-step-definition-snippets-from-the-output)
  - [Copy and Paste the Step Functions](#copy-and-paste-the-step-functions)
  - [Burpless Step Functions](#burpless-step-functions)
    - [State](#state)
    - [Type Hints](#type-hints)
  - [Update the Step Functions to Pass the Test](#update-the-step-functions-to-pass-the-test)
- [License](#license)

## Usage

### Add the Dependency
#### Deps:
Add it to your `deps.edn`:
```clojure
{:deps {net.clojars.danielmiladinov/burpless {:mvn/version "1.0.0-alpha1"}}}
```
#### Lein/Boot:
Add it to your `project.clj`:
```clojure
[net.clojars.danielmiladinov/burpless "0.1.0"]
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
    """edn
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
    """edn
    {:message "Hello, Burpless!"
     :stars 5
     :highs-and-lows [[81 49] [88 54] [76 56] [70 48] [81 55]]
     :ready-to-check? true}
    """

Undefined scenarios:
file:///path/to/my-first.feature:3 # Learning to use Burpless

1 Scenarios (1 undefined)
5 Steps (4 skipped, 1 undefined)
0m0.062s


You can implement missing steps with the snippets below:

(step :Given "I have a string value of {string} under the {keyword} key in my state"
      (fn [state ^String string ^clojure.lang.Keyword keyword]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Given "I have a long value of {int} under the {keyword} key in my state"
      (fn [state ^Integer int1 ^clojure.lang.Keyword keyword]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Given "I have a table of the following high and low temperatures:"
      (fn [state ^io.cucumber.datatable.DataTable dataTable]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :When "I am ready to check my state"
      (fn [state]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Then "my state should be equal to the following Clojure literal:"
      (fn [state ^clojure.lang.IObj fromDocString]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))



FAIL in (my-first-feature) (my_first_feature_test.clj:9)
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
(step :Given "I have a string value of {string} under the {keyword} key in my state"
      (fn [state ^String string ^clojure.lang.Keyword keyword]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Given "I have a long value of {int} under the {keyword} key in my state"
      (fn [state ^Integer int1 ^clojure.lang.Keyword keyword]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Given "I have a table of the following high and low temperatures:"
      (fn [state ^io.cucumber.datatable.DataTable dataTable]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :When "I am ready to check my state"
      (fn [state]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

(step :Then "my state should be equal to the following Clojure literal:"
      (fn [state ^clojure.lang.IObj fromDocString]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (io.cucumber.java.PendingException.))))

```
If you are following closely, you'll see some things that may not immediately make sense:
- Why does each step function take a first argument called `state`?
- Why are there type hints on all of the other step function argumnents? Are they necessary?
Let's try to answer these questions in the next section.

### Burpless Step Functions
For every call to `run-cucumber`, Burpless instantiates a Cucumber Backend as well as a Cucumber runtime, with which
to test your feature. To keep things simple, it expects to run only a single feature at a time. For your convenience,
`run-cucumber` also maintains an [`atom`](https://clojure.org/reference/atoms) to hold all of the state against which
your step functions will be executed.

We use the burpless macro, `step`, to define our step functions. It takes three parameters:
- A Clojure keyword representing one of the [Gherkin keywords](https://cucumber.io/docs/gherkin/reference/#keywords)
  for steps, e.g. [Given](https://cucumber.io/docs/gherkin/reference/#given),
  [When](https://cucumber.io/docs/gherkin/reference/#when), [Then](https://cucumber.io/docs/gherkin/reference/#then),
  [And](https://cucumber.io/docs/gherkin/reference/#and-but), [But](https://cucumber.io/docs/gherkin/reference/#and-but)
- A string representing either a [CucumberExpression](https://github.com/cucumber/cucumber-expressions#readme) (preferred) or
[RegularExpression](https://en.wikipedia.org/wiki/Regular_expression) pattern to match for the step
  - (all regular expression patterns must start with `^` and end with `$` or they will be interpreted as
    cucumber expressions by the Cucumber runtime.)
- The function to call when executing the step. Every step function will receive the current value of the state atom
as its first argument. Any output parameters (`CucumberExpression`) or capture groups (`RegularExpression`) matched in
the pattern, zero or more, are provided as additional arguments to the function. Finally, a step may also contain a
[Step Argument](https://cucumber.io/docs/gherkin/reference/#step-arguments). The step argument can either be a
[Doc String](https://cucumber.io/docs/gherkin/reference/#doc-strings) or a
[Data Table](https://cucumber.io/docs/gherkin/reference/#data-tables).
Step arguments are great for when you need to provide more data
than can comfortably fit into a single line of the feature file.

#### State
Each step function takes as its first argument the current value of the contents of the state atom. All the additional
arguments, if any, come from the arguments parsed from the feature file. It is your responsibility as the step function
author to return the new / next value of the state from each step function.

You are free to write your feature tests any way you like, but a typical approach is to follow the
[“Arrange, Act, Assert”](https://wiki.c2.com/?ArrangeActAssert) pattern.

- Arrange: Some of your step functions will build up a certain value in state, or maybe perform certain external
  initializations as side-effects.
- Act: Using the current value of state, prepare your input data needed to pass into and / or call the system under test.
  Typically you would observe the return value and add that to the state as well before returning.
- Assert: Based on whatever rules in your system about the relationship between inputs into and outputs coming from the
  system under test, make assertions about the expected output value, compared to the actual output value.

#### Type Hints
Cucumber-JVM provides snippets for you, for every step in the feature file that wasn't matched to one of your step functions.
They are not generated by burpless but come from the underlying java code itself.
Burpless receives the data about unmatched step, including, among other things:
- the keyword
- the step expression string
- the suggested method / function name to use
- the map of arguments that the function should accept (argument name -> argument type)

Burpless is responsible for taking these inputs and formatting them into a snippet you can use to quickly get started
implementing your step functions. Since burpless receives the arguments map, we know what type they will be when
Cucumber-JVM calls your step functions. For your convenience, the step function snippets contain
[type hints](https://clojure.org/reference/java_interop#typehints) derived from the step argument information provided
by Cucumber-JVM. Of course, they are optional and you a free to remove them, if you so choose. Just be aware that
removing the type hints might negatively impact test execution performance somewhat.

### Update the Step Functions to Pass the Test
While you might be able to come up with something slightly different, here's one possible implementation
for the step functions that makes the test pass:
```clojure
(ns my-first-feature-test
  (:require [clojure.test :refer [deftest is]]
            [burpless :refer [run-cucumber step]])
  (:import (clojure.lang IObj Keyword)
           (io.cucumber.datatable DataTable)
           (java.lang.reflect Type)))

(def steps
  [(step :Given "I have a string value of {string} under the {keyword} key in my state"
         (fn [state ^String string ^Keyword kw]
           (assoc state kw string)))

   (step :Given "I have a long value of {long} under the {keyword} key in my state"
         (fn [state ^Long long-value ^Keyword kw]
           (assoc state kw long-value)))

   (step :Given "I have a table of the following high and low temperatures:"
         (fn [state ^DataTable data-table]
           (assoc state :highs-and-lows (.asLists data-table ^Type Long))))

   (step :When "I am ready to check my state"
         (fn [state]
           (assoc state :ready-to-check? true)))

   (step :Then "my state should be equal to the following Clojure literal:"
         (fn [actual-state ^IObj expected-state]
           (assert (= expected-state actual-state) (str "Expected State: " expected-state "; "
                                                        "Actual State: " actual-state))))])

(deftest my-first-feature
  (is (zero? (run-cucumber "test/my-first.feature" steps))))

```

Run the tests again:
```
$ clojure -T:build test

Running tests in #{"test"}

Testing my-first-feature-test

Scenario: Learning to use Burpless                                                     # test/my-first.feature:3
  Given I have a string value of "Hello, Burpless!" under the :message key in my state # my_first_feature_test.clj:9
  And I have a long value of 5 under the :stars key in my state                        # my_first_feature_test.clj:13
  And I have a table of the following high and low temperatures:                       # my_first_feature_test.clj:17
    | 81 | 49 |
    | 88 | 54 |
    | 76 | 56 |
    | 70 | 48 |
    | 81 | 55 |
  When I am ready to check my state                                                    # my_first_feature_test.clj:21
  Then my state should be equal to the following Clojure literal:                      # my_first_feature_test.clj:25
    """edn
    {:message "Hello, Burpless!"
     :stars 5
     :highs-and-lows [[81 49] [88 54] [76 56] [70 48] [81 55]]
     :ready-to-check? true}
    """

1 Scenarios (1 passed)
5 Steps (5 passed)
0m0.052s



Ran 1 tests containing 1 assertions.
0 failures, 0 errors.
```

Happy Cucumbering!


## License

[![License](https://img.shields.io/badge/License-Apache_2.0-yellowgreen.svg)](https://opensource.org/licenses/Apache-2.0)

    Copyright 2025 Daniel Miladinov

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
