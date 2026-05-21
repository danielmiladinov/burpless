Feature: Testing around hooks

  Scenario: Simple step execution
    Given I have a simple step

  Scenario: Step with parameter (Cucumber arg)
    Given I have a simple step
    And the number is 42

  Scenario: Step with DataTable (Cucumber arg)
    Given the following users:
      | name  | age |
      | Alice | 30  |
      | Bob   | 25  |

  Scenario: Step with DocString (Cucumber arg)
    Given the following docstring:
      """
      Hello from DocString
      """
