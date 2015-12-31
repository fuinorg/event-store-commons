Feature: Append events

Scenario: Append single again 
    Given the following streams don't exist
    | Stream Name       |
    | AppendSingleAgain |
    And I append the following events in the given order
    | Stream Name       | Expected Version   |  Event Id                             | Expected Exception |
    | AppendSingleAgain | NO_OR_EMPTY_STREAM |  e62b7f2f-5cad-4188-9c0b-e94b01df11b1 | -                  |
    When I append the following events in the given order
    | Stream Name       | Expected Version   |  Event Id                             | Expected Exception |
    | AppendSingleAgain | NO_OR_EMPTY_STREAM |  e62b7f2f-5cad-4188-9c0b-e94b01df11b1 | -                  |
    | AppendSingleAgain | ANY                |  e62b7f2f-5cad-4188-9c0b-e94b01df11b1 | -                  |
    | AppendSingleAgain | 0                  |  e62b7f2f-5cad-4188-9c0b-e94b01df11b1 | -                  |
    Then this should raise no exception

 Scenario: Append multiple again 
    Given the stream "AppendMultipleAgain" does not exist
    And I append the following events to stream "AppendMultipleAgain" 
    """
    <events>
        <event id="7ab8e400-373b-4f65-96e1-96b78a791a42">
            <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[Anything goes]]></data>
        </event>
        <event id="35ae2b63-c820-4cea-8ad6-0d25e4519390">
            <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[More to come]]></data>
        </event>
    </events>
    """
    When I append the following events to stream "AppendMultipleAgain" 
    """
    <events>
        <event id="7ab8e400-373b-4f65-96e1-96b78a791a42">
            <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[Anything goes]]></data>
        </event>
        <event id="35ae2b63-c820-4cea-8ad6-0d25e4519390">
            <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[More to come]]></data>
        </event>
    </events>
    """
    Then this should raise no exception
 