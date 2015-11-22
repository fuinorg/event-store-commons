Feature: Delete a stream

Scenario: Non existing + Any expected version
    Given the stream "delete_stream_1" does not exist
    When the stream "delete_stream_1" is hard deleted using expected version "ANY"
    Then this should be successful
 
Scenario: Non existing + No stream expected version
    Given the stream "delete_stream_2" does not exist
    When the stream "delete_stream_2" is hard deleted using expected version "NO_STREAM"
    Then this should be successful

Scenario: Non existing + Empty stream expected version
    Given the stream "delete_stream_3" does not exist
    When the stream "delete_stream_3" is hard deleted using expected version "EMPTY_STREAM"
    Then this should fail with API "StreamVersionConflictException"
    
Scenario: Non existing + Invalid expected version
    Given the stream "delete_stream_4" does not exist
    When the stream "delete_stream_4" is hard deleted using expected version "1"
    Then this should fail with API "StreamVersionConflictException"
