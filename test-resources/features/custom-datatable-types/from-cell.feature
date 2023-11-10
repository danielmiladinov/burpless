Feature: Defining Custom DataTable Types From DataTable Cells

  Scenario: My Favorite Colors
    Given that my favorite colors are:
      | red    |
      | blue   |
      | black  |
      | purple |
    Then blue is one of my favorite colors
    And green is not one of my favorite colors
