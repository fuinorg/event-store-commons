Feature: Delete a stream

Scenario: Non existing
    When the following deletes are executed
    | STREAM NAME                  | HARD DELETE | EXPECTED VERSION | EXPECTED EXCEPTION             | 
    | non_existing_delete_stream_1 | true        | ANY              | -                              |
    | non_existing_delete_stream_2 | true        | NO_STREAM        | -                              |
    | non_existing_delete_stream_3 | false       | ANY              | -                              |
    | non_existing_delete_stream_4 | false       | NO_STREAM        | -                              |
    | non_existing_delete_stream_5 | true        | EMPTY_STREAM     | StreamVersionConflictException |
    | non_existing_delete_stream_6 | true        | 1                | StreamVersionConflictException |
    | non_existing_delete_stream_7 | false       | EMPTY_STREAM     | StreamVersionConflictException |
    | non_existing_delete_stream_8 | false       | 1                | StreamVersionConflictException |
    Then this should be successful
