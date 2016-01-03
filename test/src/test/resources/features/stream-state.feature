Feature: Stream state

Scenario: All states
    Given the stream "NameDoesNotMatter" does not exist
    And the following streams are created and a single event is appended to each
    | Stream Name      | 
    | StateExisting    | 
    | StateHardDeleted | 
    | StateSoftDeleted |
    And the following deletes are executed
    | Stream Name      | Hard Delete | Expected Version | 
    | StateHardDeleted | true        | ANY              |
    | StateSoftDeleted | false       | ANY              |
    When the following state queries are executed
    | Stream Name       | Expected State | Expected Exception      | 
    | NameDoesNotMatter | -              | StreamNotFoundException |
    | StateExisting     | ACTIVE         | -                       |
    | StateHardDeleted  | HARD_DELETED   | -                       |
    | StateSoftDeleted  | -              | StreamNotFoundException |
    Then this should give the expected results
    And following streams should exist
    | Stream Name   |
    | StateExisting |
    And following streams should not exist
    | Stream Name       |
    | NameDoesNotMatter |
    | StateHardDeleted  |
    | StateSoftDeleted  |
    