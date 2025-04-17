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

import jakarta.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.test.examples.BookAddedEvent;
import jakarta.annotation.Nullable;
import org.fuin.utils4j.TestCommand;

/**
 * Appends some data to a stream.
 */
public final class AppendToStreamCommand implements TestCommand<TestContext> {

    // Creation (Initialized by Cucumber)
    // DO NOT CHANGE ORDER OR RENAME VARIABLES!

    private String streamName;

    private String expectedVersion;

    private String eventId;

    private String expectedException;

    // Initialization

    private EventStore es;

    private StreamId streamId;

    private long expectedIntVersion;

    private Class<? extends Exception> expectedExceptionClass;

    private List<CommonEvent> events;

    // Execution

    private Exception actualException;

    /**
     * Default constructor used by Cucumber.
     */
    public AppendToStreamCommand() {
        super();
    }

    /**
     * Constructor for manual creation.
     * 
     * @param streamName
     *            Uniquely identifies the stream to create.
     * @param expectedVersion
     *            The version the stream should have when being deleted.
     * @param expectedExceptionClass
     *            The exception type that is expected.
     * @param events
     *            Events to add.
     */
    public AppendToStreamCommand(@NotNull final String streamName, @Nullable final long expectedVersion,
            @Nullable final Class<? extends Exception> expectedExceptionClass,
            @NotNull final CommonEvent... events) {
        this(streamName, expectedVersion, expectedExceptionClass, EscSpiUtils.asList(events));
    }

    /**
     * Constructor for manual creation.
     * 
     * @param streamName
     *            Uniquely identifies the stream to create.
     * @param expectedVersion
     *            The version the stream should have when being deleted.
     * @param expectedExceptionClass
     *            The exception type that is expected.
     * @param events
     *            Events to add.
     */
    public AppendToStreamCommand(@NotNull final String streamName, @Nullable final long expectedVersion,
            @Nullable final Class<? extends Exception> expectedExceptionClass,
            @NotNull final List<CommonEvent> events) {
        super();
        this.streamName = streamName;
        this.expectedVersion = "" + expectedVersion;
        if (expectedExceptionClass == null) {
            this.expectedException = null;
        } else {
            this.expectedException = expectedExceptionClass.getSimpleName();
        }
        this.events = events;
    }

    @Override
    public void init(final TestContext context) {
        this.es = context.getEventStore();
        this.streamName = context.getCurrentEventStoreImplType() + "_" + streamName;
        
        expectedVersion = EscTestUtils.emptyAsNull(expectedVersion);
        expectedException = EscTestUtils.emptyAsNull(expectedException);
        eventId = EscTestUtils.emptyAsNull(eventId);

        streamId = new SimpleStreamId(streamName);
        expectedIntVersion = ExpectedVersion.no(expectedVersion);
        expectedExceptionClass = EscTestUtils.exceptionForName(expectedException);
        if (eventId == null) {
            if ((events == null) || (events.size() == 0)) {
                throw new IllegalStateException("No events set to append!");
            }
        } else {
            events = new ArrayList<>();
            final CommonEvent ce = new SimpleCommonEvent(new EventId(eventId), BookAddedEvent.TYPE,
                    new BookAddedEvent("Any", "John Doe"));
            events.add(ce);
        }

    }

    @Override
    public void execute() {
        try {
            es.appendToStream(streamId, expectedIntVersion, events);
        } catch (final Exception ex) {
            actualException = ex;
        }
    }

    @Override
    public final boolean isSuccessful() {
        return TestUtils.isExpectedType(expectedExceptionClass, actualException);
    }

    @Override
    public final String getFailureDescription() {
        return EscTestUtils.createExceptionFailureMessage(streamId.asString(), expectedExceptionClass, actualException);
    }

    @Override
    public final void verify() {
        if (!isSuccessful()) {
            throw new RuntimeException(getFailureDescription());
        }
    }

    @Override
    public String toString() {
        return "AppendToStreamCommand [streamName=" + streamName + ", expectedVersion=" + expectedVersion
                + ", expectedException=" + expectedException + ", actualException=" + actualException + "]";
    }

}
