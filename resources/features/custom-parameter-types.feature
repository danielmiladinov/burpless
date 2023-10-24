Feature: Defining Custom Parameter Types

  Scenario: Custom Parameter Type: bar-map
    Given my state starts out as an empty map
    And burpless innately supports keyword parameter types
    And I want to add a bar-map of 42 under the :foo key of my state
    Then my state should look like this
    """
      {:foo {:bar 42}}
    """
