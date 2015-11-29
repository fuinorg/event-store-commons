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

import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;

/**
 * Reads a stream forward.
 */
public final class ReadForwardCommand implements TestCommand {

    // Creation

    private String streamName;

    private int start;

    private int count;

    private String expectedException;

    // Initialization

    private StreamId streamId;

    private EventStoreSync es;

    private Class<? extends Exception> expectedExceptionClass;

    // Execution

    private Exception actualException;

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
     * @param expectedException
     *            The exception that is expected, an empty string or "-".
     */
    public ReadForwardCommand(@NotNull final String streamName, final int start, final int count,
            final String expectedException) {
        super();
        this.streamName = streamName;
        this.start = start;
        this.count = count;
        this.expectedException = expectedException;
    }

    /**
     * Initializes the command before execution.
     * 
     * @param eventstore
     *            Event store to use.
     */
    public final void init(@NotNull final EventStoreSync eventstore) {
        this.es = eventstore;

        streamName = EscTestUtils.emptyAsNull(streamName);
        expectedException = EscTestUtils.emptyAsNull(expectedException);

        streamId = new SimpleStreamId(streamName, true);
        expectedExceptionClass = EscTestUtils.exceptionForSimpleName(expectedException);

    }

    @Override
    public final void execute() {
        try {
            es.readEventsForward(streamId, start, count);
        } catch (final Exception ex) {
            this.actualException = ex;
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
    public final String toString() {
        return "ReadForwardCommand [streamName=" + streamName + ", start=" + start + ", count=" + count
                + ", expectedException=" + expectedException + ", actualException="
                + actualException + "]";
    }

}
