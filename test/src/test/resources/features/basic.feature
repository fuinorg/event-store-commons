Feature: Append to a stream 

Scenario: Create a stream  
   Given The stream "Abc" does not exist
   When I write the following events to stream "Abc"
       """
        <events>
            <event id="f6a8d009-5613-4e4d-9e46-15a30daa2d28">
                <data type="MyEvent" mime-type="application/xml; encoding=utf-8">
                    <![CDATA[<book-added-event name="Shining" author="Stephen King"/>]]>
                </data>
                <meta type="MyMeta" mime-type="application/json; encoding=utf-8">
                    <![CDATA[{ "ip" : "127.0.0.1", "user" : "peter" }]]>
                </meta>
            </event>
        </events>
       """
   Then reading all events from stream "Abc" should return the following slices
        """
        <slices>
            <slice from-stream-no="0" next-stream-no="1" end-of-stream="true">
                <event id="f6a8d009-5613-4e4d-9e46-15a30daa2d28">
                    <data type="MyEvent" mime-type="application/xml; encoding=utf-8">
                        <![CDATA[<book-added-event name="Shining" author="Stephen King"/>]]>
                    </data>
                    <meta type="MyMeta" mime-type="application/json; encoding=utf-8">
                        <![CDATA[{ "ip" : "127.0.0.1", "user" : "peter" }]]>
                    </meta>
                </event>
            </slice>
        </slices>
       """
 