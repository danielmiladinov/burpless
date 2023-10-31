Feature: Defining Custom DataTable Types From DataTable Rows

  Scenario: Calculating Averages
    Given I have the following rows of test scores
      | 88 | 78 | 92 | 67 | 85 | 93 |
      | 78 | 58 | 72 | 87 | 65 | 83 |
      | 98 | 97 | 84 | 92 | 95 | 94 |
    When I calculate the averages of each row
    Then my state should look like this:
    """
      {:score-lists [[88 78 92 67 85 93]
                     [78 58 72 87 65 83]
                     [98 97 84 92 95 94]]
       :averages    [83 73 93]}
    """
