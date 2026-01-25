Feature: Testing clojure.test/is in step functions

  Scenario: Successful assertion
    Given I have a number 42
    Then the number should be 42

  Scenario: Failed assertion
    Given I have a number 42
    Then the number should be 43 and this step should fail

  Scenario: Multiple conditions with failures
    Given I have a number 42
    Then the number should satisfy multiple conditions
