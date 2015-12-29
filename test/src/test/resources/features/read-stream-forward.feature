Feature: Read a stream forward

Scenario: Illegal start or count 
    When I read forward from the following streams
    | Stream Name          | Start | Count | Expected Exception                                   | Expected Message                                               |
    | name_does_not_matter | -1    | 1     | org.fuin.objects4j.common.ContractViolationException | Min value of argument 'start' is 0, but was: -1 | 
    | name_does_not_matter | 1     | 0     | org.fuin.objects4j.common.ContractViolationException | Min value of argument 'count' is 1, but was: 0  | 
    Then this should give the expected results

Scenario: Read non existing
    Given the following streams don't exist
    | Stream Name           |
    | name_does_not_matter  |
    When I read forward from the following streams
    | Stream Name          | Start | Count | Expected Exception      |
    | name_does_not_matter | 1     | 1     | StreamNotFoundException | 
    Then this should give the expected results

Scenario: Append and read single
    Given the stream "append_single_stream_fw" does not exist
    When I append the following events in the given order
    | Stream Name             | Expected Version   |  Event Id                             |
    | append_single_stream_fw | NO_OR_EMPTY_STREAM |  a3d80db0-f35e-45f0-817f-648d07ad7391 |
    Then reading forward from stream should have the following results
    | Stream Name             | Start | Count | Result From  | Result Next | End Of Stream | Result Event Id 1                    |
    | append_single_stream_fw | 0     | 1     | 0            | 1           | false         | a3d80db0-f35e-45f0-817f-648d07ad7391 |
    | append_single_stream_fw | 1     | 1     | 1            | 1           | true          | -                                    |

Scenario: Append and read multiple
    Given the stream "append_multiple_stream_fw" does not exist
    When I append the following events in the given order
    | Stream Name               | Expected Version   |  Event Id                             |
    | append_multiple_stream_fw | NO_OR_EMPTY_STREAM |  26d53452-30a8-473b-b5cc-58650265dfb3 |
    | append_multiple_stream_fw | 0                  |  6e4f3687-ccec-496b-8c22-c3848e6f2250 |
    | append_multiple_stream_fw | 1                  |  2ce79b07-020c-4908-ad6f-cd63498d4c9f |
    | append_multiple_stream_fw | 2                  |  cf27403b-f701-4288-9668-6173e9dbce71 |
    | append_multiple_stream_fw | 3                  |  ce75244f-c3f6-44a0-a263-c94abf0ac480 |
    Then reading forward from stream should have the following results
    | Stream Name               | Start | Count | Result From  | Result Next | End Of Stream | Result Event Id 1                    | Result Event Id 2                    | Result Event Id 3                    |
    | append_multiple_stream_fw | 0     | 3     | 0            | 3           | false         | 26d53452-30a8-473b-b5cc-58650265dfb3 | 6e4f3687-ccec-496b-8c22-c3848e6f2250 | 2ce79b07-020c-4908-ad6f-cd63498d4c9f |
    | append_multiple_stream_fw | 3     | 3     | 3            | 5           | true          | cf27403b-f701-4288-9668-6173e9dbce71 | ce75244f-c3f6-44a0-a263-c94abf0ac480 | -                                    |
