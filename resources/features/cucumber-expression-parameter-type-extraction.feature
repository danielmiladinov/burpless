Feature: Cucumber Expressions Parameter Type Extraction

  Scenario: Int Type Extraction
    When I have an int value of 42
    Then its int value should be 42
    And its type should be java.lang.Integer

  Scenario: Float Type Extraction
    When I have a float value of 42.1
    Then its float value should be 42.1
    And its type should be java.lang.Float

  Scenario: Word Type Extraction
    When I have a word value of fabulous
    Then its word value should be fabulous
    And its type should be java.lang.String

  Scenario: String Type Extraction
    When I have a string value of "multiple words in a string"
    Then its string value should be "multiple words in a string"
    And its type should be java.lang.String

  Scenario: Anonymous Type Extraction
    When I have an anonymous value of anything can go in here
    Then its anonymous value should be anything can go in here
    And its type should be java.lang.String

  Scenario: BigDecimal Type Extraction
    When I have a bigdecimal value of 456.238
    Then its bigdecimal value should be 456.238
    And its type should be java.math.BigDecimal

  Scenario: Double Type Extraction
    When I have a double value of 12.489
    Then its double value should be 12.489
    And its type should be java.lang.Double

  Scenario: BigInteger Type Extraction
    When I have a biginteger value of 234239023482
    Then its biginteger value should be 234239023482
    And its type should be java.math.BigInteger

  Scenario: Byte Type Extraction
    When I have a byte value of 10
    Then its byte value should be 10
    And its type should be java.lang.Byte

  Scenario: Short Type Extraction
    When I have a short value of 5
    Then its short value should be 5
    And its type should be java.lang.Short

  Scenario: Long Type Extraction
    When I have a long value of 7133287829
    Then its long value should be 7133287829
    And its type should be java.lang.Long
