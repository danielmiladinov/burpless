Feature: A Simple Feature

  Scenario: Successful Test
    Given some setup
    When I do a thing
    Then the setup happened
    And the before hook happened
    And the thing happened
    And the before step counter is 6
    And the after step counter is 6
