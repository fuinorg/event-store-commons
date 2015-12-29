Feature: Stream state

Scenario: All states
    Given the stream "name_does_not_matter" does not exist
    And the following streams are created and a single event is appended to each
    | Stream Name               | 
    | state_existing_stream     | 
    | state_hard_deleted_stream | 
    | state_soft_deleted_stream |
    And the following deletes are executed
    | Stream Name               | Hard Delete | Expected Version | 
    | state_hard_deleted_stream | true        | ANY              |
    | state_soft_deleted_stream | false       | ANY              |
    When the following state queries are executed
    | Stream Name               | Expected State | Expected Exception      | 
    | name_does_not_matter      | -              | StreamNotFoundException |
    | state_existing_stream     | ACTIVE         | -                       |
    | state_hard_deleted_stream | HARD_DELETED   | -                       |
    | state_soft_deleted_stream | -              | StreamNotFoundException |
    Then this should give the expected results

    