/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fuin.units4j.Units4JUtils.unmarshal;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.mem.InMemoryEventStoreSync;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

// CHECKSTYLE:OFF Test code
public class BasicFeature {

    private static final int MAX_EVENTS = 10;

    private EventStoreSync eventStore;

    @Before
    public void beforeFeature() throws MalformedURLException {
        // Use the property to select the correct implementation:
        final String type = System.getProperty(EscCucumber.SYSTEM_PROPERTY);
        if (type.equals("mem")) {
            eventStore = new InMemoryEventStoreSync(Executors.newCachedThreadPool());
        } else if (type.equals("eshttp")) {
            /*
            final ThreadFactory threadFactory = Executors.defaultThreadFactory();
            final URL url = new URL("http://127.0.0.1:2113/");
            final SerializedDataType serMetaType = new SerializedDataType("MyMeta");
            final XmlDeSerializer xmlDeSer = new XmlDeSerializer(false, BookAddedEvent.class);
            final JsonDeSerializer jsonDeSer = new JsonDeSerializer();
            final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
            registry.add(new SerializedDataType("BookAddedEvent"), "application/xml", xmlDeSer);
            registry.add(new SerializedDataType("MyMeta"), "application/json", jsonDeSer);
            eventStore = new ESHttpEventStoreSync(threadFactory, url, serMetaType, ESEnvelopeType.XML, registry,
                    registry);
             */
            throw new IllegalStateException("Unknown type: " + type);
        } else {
            throw new IllegalStateException("Unknown type: " + type);
        }
        eventStore.open();
    }

    @After
    public void afterFeature() {
        eventStore.close();
        eventStore = null;
    }

    @Given("^The stream \"([^\"]*)\" does not exist$")
    public void assertThatStreamDoesNotExist(String streamName)
            throws Throwable {
        try {
            eventStore.deleteStream(new SimpleStreamId(streamName), true);
            fail("The stream should not exist: " + streamName);
        } catch (final StreamNotFoundException ex) {
            // OK
        }
    }

    @When("^I write the following events to stream \"([^\"]*)\"$")
    public void appendEventsTo(String streamName, String eventsXml)
            throws Throwable {
        final Events toAppend = unmarshal(eventsXml, Events.class);
        eventStore.appendToStream(new SimpleStreamId(streamName),
                toAppend.asCommonEvents(BookAddedEvent.class));
    }

    @Then("^reading all events from stream \"([^\"]*)\" should return the following slices$")
    public void assertReadingAllEvents(String streamName, String slicesXml)
            throws Throwable {
        final Slices expected = unmarshal(slicesXml, Slices.class);
        final Slices actual = new Slices();
        StreamEventsSlice slice = eventStore.readEventsForward(
                new SimpleStreamId(streamName), 0, MAX_EVENTS);
        while (!slice.isEndOfStream()) {
            actual.append(Slice.valueOf(slice));
            slice = eventStore.readEventsForward(
                    new SimpleStreamId(streamName), slice.getNextEventNumber(),
                    MAX_EVENTS);
        }
        actual.append(Slice.valueOf(slice));
        assertThat(actual.getSlices()).isEqualTo(expected.getSlices());
    }

}
// CHECKSTYLE:ON
