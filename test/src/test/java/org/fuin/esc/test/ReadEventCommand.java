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

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.test.examples.BookAddedEvent;
import org.fuin.units4j.Units4JUtils;

/**
 * Reads a single event from a stream.
 */
public final class ReadEventCommand implements TestCommand {

    // Creation (Initialized by Cucumber)
    // DO NOT CHANGE ORDER OR RENAME VARIABLES!

    private String streamName;

    private int eventNumber;

    private String expectedException;

    private String expectedEventXml;

    // Initialization

    private EventStoreSync es;

    private StreamId streamId;

    private Class<? extends Exception> expectedExceptionClass;

    private CommonEvent expectedEvent;

    // Execution

    private Exception actualException;

    private CommonEvent actualEvent;

    /**
     * Default constructor used by Cucumber.
     */
    public ReadEventCommand() {
        super();
    }

    /**
     * Constructor for manual creation.
     * 
     * @param streamName
     *            Uniquely identifies the stream to read.
     * @param eventNumber
     *            Number of the event to read.
     * @param expectedEventXml
     *            The expected event XML.
     * @param expectedException
     *            The exception that is expected, an empty string or "-".
     */
    public ReadEventCommand(@NotNull final String streamName, final int eventNumber,
            final String expectedEventXml, final String expectedException) {
        super();
        this.streamName = streamName;
        this.eventNumber = eventNumber;
        this.expectedEventXml = expectedEventXml;
        this.expectedException = expectedException;
    }

    /**
     * Initializes the command before execution.
     * 
     * @param eventstore
     *            Event store to use.
     */
    public void init(@NotNull final EventStoreSync eventstore) {
        this.es = eventstore;

        streamName = EscTestUtils.emptyAsNull(streamName);
        expectedException = EscTestUtils.emptyAsNull(expectedException);
        expectedEventXml = EscTestUtils.emptyAsNull(expectedEventXml);

        streamId = new SimpleStreamId(streamName);
        expectedExceptionClass = EscTestUtils.exceptionForName(expectedException);        
        final Event event;
        if (expectedEventXml == null) {
            event = null;
            expectedEvent = null;
        } else {
            event = Units4JUtils.unmarshal(expectedEventXml, Event.class);
            expectedEvent = event.asCommonEvent(BookAddedEvent.class);
        }

    }

    @Override
    public void execute() {
        try {
            actualEvent = es.readEvent(streamId, eventNumber);
        } catch (final Exception ex) {
            actualException = ex;
        }
    }

    @Override
    public final boolean isSuccessful() {
        if (!EscTestUtils.isExpectedType(expectedExceptionClass, actualException)) {
            return false;
        }
        return EscTestUtils.sameContent(expectedEvent, actualEvent);
    }

    @Override
    public final String getFailureDescription() {
        if (!EscTestUtils.isExpectedType(expectedExceptionClass, actualException)) {
            return EscTestUtils.createExceptionFailureMessage(streamId, expectedExceptionClass,
                    actualException);
        }
        return EscTestUtils.createExceptionFailureMessage(streamId, expectedEvent, actualEvent);
    }

    @Override
    public final void verify() {
        if (!isSuccessful()) {
            throw new RuntimeException(getFailureDescription());
        }
    }

    @Override
    public final String toString() {
        return "DeleteCommand [streamName=" + streamName + ", eventNumber=" + eventNumber
                + ", expectedEvent=" + expectedEvent + ", actualEvent=" + actualEvent
                + ", expectedException=" + expectedException + ", actualException=" + actualException + "]";
    }

}
