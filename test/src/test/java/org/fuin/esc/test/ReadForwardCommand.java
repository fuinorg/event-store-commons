/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.test;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.test.examples.BookAddedEvent;
import javax.annotation.Nullable;

/**
 * Reads a stream forward.
 */
public final class ReadForwardCommand implements TestCommand {

    // Creation (Initialized by Cucumber)
    // DO NOT CHANGE ORDER OR RENAME VARIABLES!

    private String streamName;

    private int start;

    private int count;

    private int resultFrom;

    private int resultNext;

    private boolean endOfStream;

    private String resultEventId1;

    private String resultEventId2;

    private String resultEventId3;

    private String resultEventId4;

    private String resultEventId5;

    private String resultEventId6;

    private String resultEventId7;

    private String resultEventId8;

    private String resultEventId9;

    // Initialization

    private StreamId streamId;

    private EventStore es;

    private StreamEventsSlice expectedSlice;

    // Execution

    private Exception actualException;

    private StreamEventsSlice actualSlice;

    /**
     * Default constructor used by Cucumber.
     */
    public ReadForwardCommand() {
        super();
    }

    /**
     * Constructor for manual creation.
     * 
     * @param streamName
     *            Uniquely identifies the stream to create.
     * @param start
     *            The starting point to read from.
     * @param count
     *            The count of items to read.
     * @param fromEventNumber
     *            The starting point (represented as a sequence number) of the read.
     * @param nextEventNumber
     *            The next event number that can be read.
     * @param endOfStream
     *            Determines whether or not this is the end of the stream.
     * @param events
     *            Expected events.
     */
    public ReadForwardCommand(@NotNull final String streamName, final int start, final int count,
            final int fromEventNumber, final int nextEventNumber, final boolean endOfStream,
            @Nullable final String... events) {
        super();
        this.streamName = streamName;
        this.start = start;
        this.count = count;
        this.resultFrom = fromEventNumber;
        this.resultNext = nextEventNumber;
        this.endOfStream = endOfStream;
        final List<CommonEvent> expectedEvents = new ArrayList<>();
        if (events != null) {
            for (final String event : events) {
                addEvent(expectedEvents, event);
            }
        }
        this.expectedSlice = new StreamEventsSlice(fromEventNumber, expectedEvents, nextEventNumber,
                endOfStream);
    }

    @Override
    public void init(final String currentEventStoreImplType, final EventStore eventstore) {
        this.es = eventstore;
        this.streamName = currentEventStoreImplType + "_" + streamName;

        resultEventId1 = EscTestUtils.emptyAsNull(resultEventId1);
        resultEventId2 = EscTestUtils.emptyAsNull(resultEventId2);
        resultEventId3 = EscTestUtils.emptyAsNull(resultEventId3);
        resultEventId4 = EscTestUtils.emptyAsNull(resultEventId4);
        resultEventId5 = EscTestUtils.emptyAsNull(resultEventId5);
        resultEventId6 = EscTestUtils.emptyAsNull(resultEventId6);
        resultEventId7 = EscTestUtils.emptyAsNull(resultEventId7);
        resultEventId8 = EscTestUtils.emptyAsNull(resultEventId8);
        resultEventId9 = EscTestUtils.emptyAsNull(resultEventId9);

        streamId = new SimpleStreamId(streamName);
        final List<CommonEvent> expectedEvents = new ArrayList<>();
        addEvent(expectedEvents, resultEventId1);
        addEvent(expectedEvents, resultEventId2);
        addEvent(expectedEvents, resultEventId3);
        addEvent(expectedEvents, resultEventId4);
        addEvent(expectedEvents, resultEventId5);
        addEvent(expectedEvents, resultEventId6);
        addEvent(expectedEvents, resultEventId7);
        addEvent(expectedEvents, resultEventId8);
        addEvent(expectedEvents, resultEventId9);
        expectedSlice = new StreamEventsSlice(resultFrom, expectedEvents, resultNext, endOfStream);

    }

    private static void addEvent(final List<CommonEvent> events, final String eventId) {
        if (eventId != null) {
            final CommonEvent ce = new SimpleCommonEvent(new EventId(eventId), BookAddedEvent.TYPE,
                    new BookAddedEvent("Any", "John Doe"));
            events.add(ce);
        }
    }

    @Override
    public final void execute() {
        try {
            actualSlice = es.readEventsForward(streamId, start, count);
        } catch (final Exception ex) {
            this.actualException = ex;
        }
    }

    @Override
    public final boolean isSuccessful() {
        if (actualException != null) {
            return false;
        }
        return expectedSlice.equals(actualSlice);
    }

    @Override
    public final String getFailureDescription() {
        if (actualException != null) {
            return EscTestUtils.createExceptionFailureMessage(streamId, actualException);
        }
        if (actualSlice == null) {
            return "[" + streamId + "] expected " + expectedSlice.toDebugString() + ", but was: null";
        }
        return "[" + streamId + "] expected " + expectedSlice.toDebugString() + ", but was: "
                + actualSlice.toDebugString();
    }

    @Override
    public final void verify() {
        if (!isSuccessful()) {
            throw new RuntimeException(getFailureDescription());
        }
    }

    @Override
    public final String toString() {
        return "ReadForwardCommand [streamName=" + streamName + ", start=" + start + ", count=" + count
                + ", expectedSlice=" + expectedSlice + ", actualSlice=" + actualSlice + "]";
    }

}
