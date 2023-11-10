Feature: Burpless Accepts Args as a Map

  There are certain args that take parameters, and others that don't.
  For compatibility with both types of args, the arg names are taken from the keys of the arg map,
  and for args that take parameters, the arg parameters come from the arg map values.

  For args that do not take parameters, the arg map values are ignored.

  Scenario Outline: Single Arg That Takes Parameters
    Given the args map begins as an empty map
    When the <arg-name> arg is set to <arg-value>
    Then the args output should be equal to <args-output>

    Examples:
      | arg-name        | arg-value                         | args-output                                        |
      | :threads        | "11"                              | "--threads 11"                                     |
      | :threads        | "13"                              | "--threads 13"                                     |
      | :threads        | "17"                              | "--threads 17"                                     |
      | :glue           | "com.example.app"                 | "--glue com.example.app"                           |
      | :glue           | "com.foo.bar.baz"                 | "--glue com.foo.bar.baz"                           |
      | :glue           | "org.serious.app"                 | "--glue org.serious.app"                           |
      | :plugin         | "pretty"                          | "--plugin pretty"                                  |
      | :plugin         | "html"                            | "--plugin html"                                    |
      | :plugin         | "timeline"                        | "--plugin timeline"                                |
      | :tags           | "@fast"                           | "--tags @fast"                                     |
      | :tags           | "@wip and not @slow"              | "--tags @wip and not @slow"                        |
      | :tags           | "@smoke and @fast"                | "--tags @smoke and @fast"                          |
      | :tags           | "@gui or @database"               | "--tags @gui or @database"                         |
      | :name           | "\".*experimental.*\""            | "--name \".*experimental.*\""                      |
      | :name           | "\".*legacy.*\""                  | "--name \".*legacy.*\""                            |
      | :snippets       | "underscore"                      | "--snippets underscore"                            |
      | :snippets       | "camelcase"                       | "--snippets camelcase"                             |
      | :i18n           | "en-lol"                          | "--i18n en-lol"                                    |
      | :i18n           | "sr-Latn"                         | "--i18n sr-Latn"                                   |
      | :i18n           | "sr-Cyrl"                         | "--i18n sr-Cyrl"                                   |
      | :i18n           | "ru"                              | "--i18n ru"                                        |
      | :order          | "reverse"                         | "--order reverse"                                  |
      | :order          | "random"                          | "--order random"                                   |
      | :order          | "random:12345"                    | "--order random:12345"                             |
      | :count          | "15"                              | "--count 15"                                       |
      | :count          | "25"                              | "--count 25"                                       |
      | :object-factory | "com.example.app.ObjectFactory"   | "--object-factory com.example.app.ObjectFactory"   |
      | :object-factory | "com.foo.bar.baz.FabrikaObjekata" | "--object-factory com.foo.bar.baz.FabrikaObjekata" |
      | :uuid-generator | "com.foo.bar.baz.UuidGenerator"   | "--uuid-generator com.foo.bar.baz.UuidGenerator"   |
      | :uuid-generator | "com.example.app.NeoUuidMaker"    | "--uuid-generator com.example.app.NeoUuidMaker"    |


  Scenario Outline: Single Arg That Takes No Parameters
    Given the args map begins as an empty map
    When the <arg-name> arg is set to nil
    Then the args output should be equal to <args-output>

    Examples:
      | arg-name    | args-output    |
      | :dry-run    | "--dry-run"    |
      | :monochrome | "--monochrome" |
      | :version    | "--version"    |
      | :help       | "--help"       |
      | :wip        | "--wip"        |


  Scenario Outline: Single Short Arg
    Given the args map begins as an empty map
    When the <arg-name> arg is set to <arg-value>
    Then the args output should be equal to <args-output>

    Examples:
      | arg-name | arg-value                   | args-output             |
      | :g       | "com.example.app"           | "-g com.example.app"    |
      | :g       | "com.foo.bar.baz"           | "-g com.foo.bar.baz"    |
      | :g       | "org.serious.app"           | "-g org.serious.app"    |
      | :p       | "pretty"                    | "-p pretty"             |
      | :p       | "html"                      | "-p html"               |
      | :p       | "timeline"                  | "-p timeline"           |
      | :t       | "@fast"                     | "-t @fast"              |
      | :t       | "@wip and not @slow"        | "-t @wip and not @slow" |
      | :t       | "@smoke and @fast"          | "-t @smoke and @fast"   |
      | :t       | "@gui or @database"         | "-t @gui or @database"  |
      | :d       | "whatever, it don't matter" | "-d"                    |
      | :m       | "whatever, it don't matter" | "-m"                    |
      | :v       | "whatever, it don't matter" | "-v"                    |
      | :h       | "whatever, it don't matter" | "-h"                    |
      | :w       | "whatever, it don't matter" | "-w"                    |


  Scenario Outline: Single Invertible Arg
    Given the args map begins as an empty map
    When the <arg-name> arg is set to nil
    Then the args output should be equal to <args-output>

    Examples:
      | arg-name       | args-output       |
      | :no-dry-run    | "--no-dry-run"    |
      | :no-monochrome | "--no-monochrome" |


    Scenario: Multiple Args Example
      Given the args map begins as an empty map
      When the args are set to the following edn:
      """edn
      {:threads        4
       :g              "com.example.app"
       :plugin         "teamcity"
       :tags           "@smoke and @fast"
       :name           ".*experimental.*"
       :w              nil
       :no-dry-run     nil
       :no-monochrome  nil
       :snippets       "underscore"
       :h              nil
       :order          "random"
       :count          "25"
       :v              nil
       :object-factory "com.example.app.ObjectFactory"}
      """
      Then the args output should be equal to
      """multiline-trimmed-string
      --count 25
      -g com.example.app
      -h
      --name .*experimental.*
      --no-dry-run
      --no-monochrome
      --object-factory com.example.app.ObjectFactory
      --order random
      --plugin teamcity
      --snippets underscore
      --tags @smoke and @fast
      --threads 4
      -v
      -w
      """

