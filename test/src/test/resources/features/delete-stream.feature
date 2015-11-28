Feature: Delete a stream

Scenario: Non existing
    When the following deletes are executed
    | Stream Name           | Hard Delete | Expected Version   | Expected Exception             | 
    | non_existing_stream_1 | true        | ANY                | -                              |
    | non_existing_stream_2 | true        | NO_OR_EMPTY_STREAM | -                              |
    | non_existing_stream_3 | false       | ANY                | -                              |
    | non_existing_stream_4 | false       | NO_OR_EMPTY_STREAM | -                              |
    | non_existing_stream_6 | true        | 1                  | StreamVersionConflictException |
    | non_existing_stream_8 | false       | 1                  | StreamVersionConflictException |
    Then this should be successful
