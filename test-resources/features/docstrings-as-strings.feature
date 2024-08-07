Feature: Step Functions Receive DocStrings as Strings

  Due to how `DocString`s get autowired as `String` instances for appropriately-annotated Java method step
  “functions”, the cucumber-jvm `SnippetGenerator` defaults to typing docString arguments as `String`,
  even though they are passed to Burpless' `StepDefinition` implementations as instances of
  `io.cucumber.docstring.DocString` – forcing step function writers to first call `(.getContent docString)`
  before they could access the actual `String` value.
  Burpless should be able to do this on step function writers' behalf.

  Scenario: Receive DocStrings as Strings, Part 1
    Given the expected doc-string value is "foo"
    When I extract the doc-string value from this step:
      """
      foo
      """
    Then the actual extracted value should be of type "java.lang.String"
    And the actual extracted doc-string value should match the expected doc-string value

  Scenario: Receive DocStrings as Strings, Part 2
    Given the expected doc-string value is "bar"
    When I extract the doc-string value from this step:
      """
      bar
      """
    Then the actual extracted value should be of type "java.lang.String"
    And the actual extracted doc-string value should match the expected doc-string value

  Scenario: Receive DocStrings as Strings, Part 3
    Given the expected doc-string value is "baz"
    When I extract the doc-string value from this step:
      """
      baz
      """
    Then the actual extracted value should be of type "java.lang.String"
    And the actual extracted doc-string value should match the expected doc-string value
