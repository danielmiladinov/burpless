Feature: Defining Custom DataTable Types From The Entire DataTable

  Scenario: TicTacToe
    Given I have the following tic-tac-toe board:
      |   |   | o |
      |   | x | o |
      | x |   |   |
    * the :topLeft square should be empty
    * the :topMid square should be empty
    * the :topRight square should be :o
    * the :midLeft square should be empty
    * the :midMid square should be :x
    * the :midRight square should be :o
    * the :botLeft square should be :x
    * the :botMid square should be empty
    * the :botRight square should be empty
