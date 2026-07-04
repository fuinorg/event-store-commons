Feature: Up-cast an event on read

  An event that was appended at an older version is read back converted to the latest in-memory
  representation when the backend is built with a converter registry. The in-memory store keeps the live
  objects and never (de)serializes, so it is skipped.

  Scenario: An event stored at version 1 is read back up-cast to version 2
    Given the backend deserializes stored events
    And the stream "UpcastGreet" does not exist
    When I append the following events to stream "UpcastGreet"
    """
    <events>
        <event id="a4b1e2c0-1111-4a2b-8c3d-000000000001">
            <data type="GreetEvent" mime-type="application/xml; version=1; encoding=utf-8"><![CDATA[<greet-event name="World"/>]]></data>
            <meta type="MyMeta" mime-type="application/json; encoding=utf-8"><![CDATA[{ "a" : "1" }]]></meta>
        </event>
    </events>
    """
    Then this should raise no exception
    And reading event 0 from stream "UpcastGreet" should return the following event
    """
    <event id="a4b1e2c0-1111-4a2b-8c3d-000000000001">
        <data type="GreetEvent" mime-type="application/xml; version=2; encoding=utf-8"><![CDATA[<greet-event greeting="Hello, World"/>]]></data>
        <meta type="MyMeta" mime-type="application/json; encoding=utf-8"><![CDATA[{ "a" : "1" }]]></meta>
    </event>
    """
