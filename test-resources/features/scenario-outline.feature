Feature: Scenario outline support
  In this example, we'll borrow
  from the [Cucumber 10 Minute Tutorial](https://cucumber.io/docs/guides/10-minute-tutorial/?lang=java)

  Scenario Outline: Today is or is not Friday
    Given today is "<today>"
    When I ask whether it's Friday yet
    Then I should be told "<answer>"

    Examples:
      | today          | answer                       |
      | Sunday         | Nope                         |
      | Monday         | Nope                         |
      | Tuesday        | Nope                         |
      | Wednesday      | Nope                         |
      | Thursday       | Nope                         |
      | Friday         | TGIF!                        |
      | Saturday       | Nope                         |
      | anything else! | That's not a day I recognize |
