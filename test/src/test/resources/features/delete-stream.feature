Feature: Delete a stream

Scenario: A stream which doesn't exist should success when passed empty stream expected version
    Given the stream "delete_stream_1" does not exist
    When the stream "delete_stream_1" is hard deleted using expected version "EMPTY_STREAM"
    Then this should be successful
 
