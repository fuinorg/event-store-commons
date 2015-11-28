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
    Given the following streams are created and a single event is appended 
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
