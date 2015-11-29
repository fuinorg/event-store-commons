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
 * Tests for stream existance.
 */
public final class StreamExistsCommand implements TestCommand {

    // Creation

    private String streamName;

    private boolean shouldExist;

    // Initialization

    private StreamId streamId;

    private EventStoreSync es;

    // Execution

    private Exception actualException;

    private boolean actuallyExists;

    /**
     * Default constructor used by Cucumber.
     */
    public StreamExistsCommand() {
        super();
    }

    /**
     * Constructor for manual creation.
     * 
     * @param streamName
     *            Uniquely identifies the stream to create.
     * @param shouldExist
     *            Determines if the stream should exist.
     */
    public StreamExistsCommand(@NotNull final String streamName, final boolean shouldExist) {
        super();
        this.streamName = streamName;
        this.shouldExist = shouldExist;
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

        this.streamId = new SimpleStreamId(streamName, false);

    }

    @Override
    public final void execute() {
        try {
            actuallyExists = es.streamExists(streamId);
        } catch (final Exception ex) {
            this.actualException = ex;
        }
    }

    @Override
    public final boolean isSuccessful() {
        return EscTestUtils.isExpectedType(null, actualException) && (actuallyExists == shouldExist);
    }

    @Override
    public final String getFailureDescription() {
        if (!EscTestUtils.isExpectedType(null, actualException)) {
            return EscTestUtils.createExceptionFailureMessage(streamId, null, actualException);
        }
        if (shouldExist) {
            return "[" + streamId + "] The stream should exist, but does not";
        }
        return "[" + streamId + "] The stream should NOT exist, but it does";
    }

    @Override
    public final void verify() {
        if (!isSuccessful()) {
            throw new RuntimeException(getFailureDescription());
        }
    }

    @Override
    public final String toString() {
        return "CreateStreamCommand [streamName=" + streamName + ", shouldExist=" + shouldExist
                + ", actuallyExists=" + actuallyExists + ", actualException=" + actualException + "]";
    }

}
