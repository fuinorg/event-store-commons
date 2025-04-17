Feature: Read a single event

  Scenario: Same type append and read
    Given the stream "AppendSameAndRead" does not exist
    When I append the following events to stream "AppendSameAndRead" 
    """
    <events>
        <event id="73d2ac98-04cf-4531-bd88-0da46e394a02">
            <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[Anything goes]]></data>
            <meta type="MyMeta" mime-type="application/json; encoding=utf-8"><![CDATA[{ "a" : "1" }]]></meta>
        </event>
        <event id="668879c5-7a86-420d-8d7f-e8a66205002e">
            <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[More to come]]></data>
            <meta type="MyMeta" mime-type="application/json; encoding=utf-8"><![CDATA[{ "a" : "2" }]]></meta>
        </event>
    </events>
    """
    Then this should raise no exception
    And reading event 0 from stream "AppendSameAndRead" should return the following event
    """
    <event id="73d2ac98-04cf-4531-bd88-0da46e394a02">
        <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[Anything goes]]></data>
        <meta type="MyMeta" mime-type="application/json; encoding=utf-8"><![CDATA[{ "a" : "1" }]]></meta>
    </event>
    """
    And reading event 1 from stream "AppendSameAndRead" should return the following event
    """
    <event id="668879c5-7a86-420d-8d7f-e8a66205002e">
        <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[More to come]]></data>
        <meta type="MyMeta" mime-type="application/json; encoding=utf-8"><![CDATA[{ "a" : "2" }]]></meta>
    </event>
    """

  Scenario: Different type append and read
    Given the stream "AppendDiffAndRead" does not exist
    When I append the following events to stream "AppendDiffAndRead" 
    """
    <events>
        <event id="73d2ac98-04cf-4531-bd88-0da46e394a02">
            <data type="BookAddedEvent" mime-type="application/xml; version=1; encoding=utf-8"><![CDATA[<book-added-event><name>Shining</name><author>Stephen King</author></book-added-event>]]></data>
            <meta type="MyMeta" mime-type="application/json; encoding=utf-8"><![CDATA[{ "a" : "1" }]]></meta>
        </event>
        <event id="668879c5-7a86-420d-8d7f-e8a66205002e">
            <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[More to come]]></data>
            <meta type="MyMeta" mime-type="application/json; encoding=utf-8"><![CDATA[{ "a" : "2" }]]></meta>
        </event>
    </events>
    """
    Then reading event 0 from stream "AppendDiffAndRead" should return the following event
    """
    <event id="73d2ac98-04cf-4531-bd88-0da46e394a02">
        <data type="BookAddedEvent" mime-type="application/xml; version=1; encoding=utf-8"><![CDATA[<book-added-event><name>Shining</name><author>Stephen King</author></book-added-event>]]></data>
        <meta type="MyMeta" mime-type="application/json; encoding=utf-8"><![CDATA[{ "a" : "1" }]]></meta>
    </event>
    """
    And reading event 1 from stream "AppendDiffAndRead" should return the following event
    """
    <event id="668879c5-7a86-420d-8d7f-e8a66205002e">
        <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[More to come]]></data>
        <meta type="MyMeta" mime-type="application/json; encoding=utf-8"><![CDATA[{ "a" : "2" }]]></meta>
    </event>
    """
    And reading event 2 from stream "AppendDiffAndRead" should throw a "EventNotFoundException"
 