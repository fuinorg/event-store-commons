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

import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.WritableEventStore;
import org.fuin.esc.mem.InMemoryEventStore;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

// CHECKSTYLE:OFF Test code
public class BasicFeature {

    private static final int MAX_EVENTS = 10;

    private WritableEventStore eventStore;

    @Before
    public void beforeFeature() {
        // Use the property to select the correct implementation:
        // System.getProperty(EscCucumber.SYSTEM_PROPERTY)
        eventStore = new InMemoryEventStore();
    }

    @After
    public void afterFeature() {
        eventStore = null;
    }

    @Given("^The stream \"([^\"]*)\" does not exist$")
    public void assertThatStreamDoesNotExist(String streamName)
            throws Throwable {
        try {
            eventStore.deleteStream(new SimpleStreamId(streamName));
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
        StreamEventsSlice slice = eventStore.readStreamEventsForward(
                new SimpleStreamId(streamName), 0, MAX_EVENTS);
        while (!slice.isEndOfStream()) {
            actual.append(Slice.valueOf(slice));
            slice = eventStore.readStreamEventsForward(new SimpleStreamId(
                    streamName), slice.getNextEventNumber(), MAX_EVENTS);
        }
        assertThat(actual.getSlices()).isEqualTo(expected.getSlices());
    }

}
//CHECKSTYLE:ON
