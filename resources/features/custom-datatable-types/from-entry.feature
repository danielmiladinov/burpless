Feature: Defining Custom DataTable Types From DataTable Entries

  Scenario: Correct non-zero number of books found by author
    Given I have the following books in the store
      | title                                | author              |
      | The Devil in the White City          | Erik Larson         |
      | The Lion, the Witch and the Wardrobe | C.S. Lewis          |
      | In the Garden of Beasts              | Erik Larson         |
      | To Kill a Mockingbird                | Harper Lee          |
      | The Catcher in the Rye               | J.D. Salinger       |
      | The Great Gatsby                     | F. Scott Fitzgerald |
    When I search for books by author "Erik Larson"
    Then I find 2 books
