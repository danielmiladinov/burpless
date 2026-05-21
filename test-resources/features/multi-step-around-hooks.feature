Feature: Testing around hooks with multiple steps

  Scenario: Multiple step execution
    Given I have a simple step
    When I do another step
    Then I verify the result