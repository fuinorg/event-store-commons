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

import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamState;

/**
 * Queries a stream state.
 */
public final class StreamStateCommand implements TestCommand {

    // Creation (Initialized by Cucumber)
    // DO NOT CHANGE ORDER OR RENAME VARIABLES!

    private String streamName;

    private String expectedState;

    private String expectedException;

    // Initialization

    private EventStore es;

    private StreamId streamId;

    private StreamState expectedStreamState;

    private Class<? extends Exception> expectedExceptionClass;

    // Execution

    private StreamState actualStreamState;

    private Exception actualException;

    /**
     * Default constructor used by Cucumber.
     */
    public StreamStateCommand() {
        super();
    }

    /**
     * Constructor for manual creation.
     * 
     * @param streamName
     *            Uniquely identifies the stream to create.
     * @param expectedState
     *            The state the stream should have, an empty string or "-".
     * @param expectedException
     *            The exception that is expected, an empty string or "-".
     */
    public StreamStateCommand(@NotNull final String streamName, final String expectedState,
            final String expectedException) {
        super();
        this.streamName = streamName;
        this.expectedState = expectedState;
        this.expectedException = expectedException;
    }

    /**
     * Initializes the command before execution.
     * 
     * @param eventstore
     *            Event store to use.
     */
    public void init(@NotNull final EventStore eventstore) {
        this.es = eventstore;

        streamName = EscTestUtils.emptyAsNull(streamName);
        expectedState = EscTestUtils.emptyAsNull(expectedState);
        expectedException = EscTestUtils.emptyAsNull(expectedException);

        streamId = new SimpleStreamId(streamName);
        if (expectedState == null) {
            expectedStreamState = null;
        } else {
            expectedStreamState = StreamState.valueOf(expectedState);
        }
        expectedExceptionClass = EscTestUtils.exceptionForName(expectedException);

    }

    @Override
    public void execute() {
        try {
            actualStreamState = es.streamState(streamId);
        } catch (final Exception ex) {
            actualException = ex;
        }
    }

    @Override
    public final boolean isSuccessful() {
        if (expectedStreamState == null) {
            if (expectedExceptionClass == null) {
                throw new IllegalStateException("Both, expected state and exception are null");
            }
            return EscTestUtils.isExpectedType(expectedExceptionClass, actualException);
        }
        if (expectedExceptionClass != null) {
            throw new IllegalStateException("Both, expected state and exception are set");
        }
        return expectedStreamState == actualStreamState;
    }

    @Override
    public final String getFailureDescription() {
        if (expectedStreamState == null) {
            if (actualStreamState != null) {
                return "[" + streamId + "] expected exception '" + expectedException + "', but got state: "
                        + actualStreamState;
            }
            return EscTestUtils.createExceptionFailureMessage(streamId, expectedExceptionClass,
                    actualException);
        }
        if (actualException != null) {
            return EscTestUtils.createExceptionFailureMessage(streamId, expectedExceptionClass,
                    actualException);
        }
        return "[" + streamId + "] expected " + expectedStreamState + ", but was: " + actualStreamState;
    }

    @Override
    public final void verify() {
        if (!isSuccessful()) {
            throw new RuntimeException(getFailureDescription());
        }
    }

    @Override
    public final String toString() {
        return "StreamState [streamName=" + streamName + ", expectedState=" + expectedState
                + ", expectedException=" + expectedException + ", actualException=" + actualException + "]";
    }

}
