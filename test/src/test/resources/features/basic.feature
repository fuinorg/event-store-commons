Feature: Append to a stream 

Scenario: Create a stream  
   Given The stream "Abc" does not exist
   When I write the following events to "Abc"
       """
        <events>
            <event id="5741bcf1-9292-446b-84c1-957ed53b8d88" type="BookAddedEvent">
                <data>
                    <book-added-event name="Shining" author="Stephen King"/>
                </data>
                <meta>
                    <ip>127.0.0.1</ip>
                </meta>
            </event>
        <events>
       """
   Then I expect that reading all from stream "Abc" returns the following events
        """
        <events>
            <event id="5741bcf1-9292-446b-84c1-957ed53b8d88" type="BookAddedEvent">
                <data>
                    <book-added-event name="Shining" author="Stephen King"/>
                </data>
                <meta>
                    <ip>127.0.0.1</ip>
                </meta>
            </event>
        <events>
       """
 