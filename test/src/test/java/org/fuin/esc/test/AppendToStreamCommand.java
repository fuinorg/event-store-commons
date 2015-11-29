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

import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.Nullable;

/**
 * Appends some data to a stream.
 */
public final class AppendToStreamCommand implements TestCommand {

    // Creation (Initialized by Cucumber)

    private String streamName;

    private String expectedVersion;

    private String expectedException;

    // Initialization

    private EventStoreSync es;

    private StreamId streamId;

    private int expectedIntVersion;

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
    public AppendToStreamCommand(@NotNull final String streamName, @Nullable final int expectedVersion,
            @Nullable final Class<? extends Exception> expectedExceptionClass,
            @NotNull final CommonEvent... events) {
        super();
        this.streamName = streamName;
        this.expectedVersion = "" + expectedVersion;
        this.expectedException = (expectedExceptionClass == null ? null : expectedExceptionClass
                .getSimpleName());
        this.events = Arrays.asList(events);
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
        expectedVersion = EscTestUtils.emptyAsNull(expectedVersion);
        expectedException = EscTestUtils.emptyAsNull(expectedException);

        streamId = new SimpleStreamId(streamName, false);
        expectedIntVersion = ExpectedVersion.no(expectedVersion);
        expectedExceptionClass = EscTestUtils.exceptionForSimpleName(expectedException);

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
        return EscTestUtils.isExpectedType(expectedExceptionClass, actualException);
    }

    @Override
    public final String getFailureDescription() {
        return EscTestUtils.createExceptionFailureMessage(streamId, expectedExceptionClass, actualException);
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