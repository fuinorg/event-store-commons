Feature: Delete a stream

Scenario: Non existing
    When the following deletes are executed
    | Stream Name                  | Hard Delete | Expected Version   | Expected Exception             | 
    | delete_non_existing_stream_1 | true        | ANY                | -                              |
    | delete_non_existing_stream_2 | true        | NO_OR_EMPTY_STREAM | -                              |
    | delete_non_existing_stream_3 | false       | ANY                | -                              |
    | delete_non_existing_stream_4 | false       | NO_OR_EMPTY_STREAM | -                              |
    | delete_non_existing_stream_5 | true        | 1                  | StreamVersionConflictException |
    | delete_non_existing_stream_6 | false       | 1                  | StreamVersionConflictException |
    Then this should be successful

Scenario: Already existing
    Given the following streams are created and a single event is appended to each
    | Stream Name              | 
    | delete_existing_stream_1 | 
    | delete_existing_stream_2 |
    | delete_existing_stream_3 |
    | delete_existing_stream_4 |
    | delete_existing_stream_5 |
    | delete_existing_stream_6 |
    | delete_existing_stream_7 |
    | delete_existing_stream_8 |
    When the following deletes are executed
    | Stream Name              | Hard Delete | Expected Version   | Expected Exception             | 
    | delete_existing_stream_1 | true        | ANY                | -                              |
    | delete_existing_stream_2 | true        | NO_OR_EMPTY_STREAM | StreamVersionConflictException |
    | delete_existing_stream_3 | false       | ANY                | -                              |
    | delete_existing_stream_4 | false       | NO_OR_EMPTY_STREAM | StreamVersionConflictException |
    | delete_existing_stream_5 | true        | 0                  | -                              |
    | delete_existing_stream_6 | false       | 0                  | -                              |
    | delete_existing_stream_7 | true        | 1                  | StreamVersionConflictException |
    | delete_existing_stream_8 | false       | 1                  | StreamVersionConflictException |
    Then this should be successful

Scenario: Read after delete
    Given the following streams are created and a single event is appended to each 
    | Stream Name            |
    | read_after_hard_delete |
    | read_after_soft_delete |
    And the following deletes are executed
    | Stream Name            | Hard Delete | Expected Version | Expected Exception | 
    | read_after_hard_delete | true        | ANY              | -                  |
    | read_after_soft_delete | false       | ANY              | -                  |
    Then following streams should not exist
    | Stream Name            |
    | read_after_hard_delete |
    | read_after_soft_delete |
    And reading forward from the following streams should have the given result
    | Stream Name            | Start | Count | Expected Exception       | 
    | read_after_hard_delete | 0     | 1     | StreamDeletedException   |
    | read_after_soft_delete | 0     | 1     | StreamNotFoundException  |
