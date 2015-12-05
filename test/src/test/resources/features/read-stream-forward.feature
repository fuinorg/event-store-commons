Feature: Read a stream forward

Scenario: Illegal start or count 
    When I read forward from the following streams
    | Stream Name          | Start | Count | Expected Exception                                   | Expected Message                                               |
    | name_does_not_matter | 0     | 1     | org.fuin.objects4j.common.ContractViolationException | Min value of argument 'start' is 1, but was: 0  | 
    | name_does_not_matter | 1     | 0     | org.fuin.objects4j.common.ContractViolationException | Min value of argument 'count' is 1, but was: 0  | 
    | name_does_not_matter | -1    | 1     | org.fuin.objects4j.common.ContractViolationException | Min value of argument 'start' is 1, but was: -1 | 
    | name_does_not_matter | 1     | -1    | org.fuin.objects4j.common.ContractViolationException | Min value of argument 'count' is 1, but was: -1 | 
    Then this should give the expected results

