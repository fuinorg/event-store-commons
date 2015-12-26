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

Scenario: Append single
    Given the stream "append_single_stream" does not exist
    When I append the following events in the given order
    | Stream Name          | Expected Version   |  Event Id                             |
    | append_single_stream | NO_OR_EMPTY_STREAM |  a3d80db0-f35e-45f0-817f-648d07ad7391 |
    Then reading forward from stream should have the following results
    | Stream Name          | Start | Count | Result From  | Result Next | End Of Stream | Result Event Id 1                    |
    | append_single_stream | 0     | 1     | 0            | 1           | false         | a3d80db0-f35e-45f0-817f-648d07ad7391 |
    | append_single_stream | 1     | 1     | 1            | 1           | true          | -                                    |

    