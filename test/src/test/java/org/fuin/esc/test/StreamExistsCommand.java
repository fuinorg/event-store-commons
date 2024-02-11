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

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.utils4j.TestCommand;

/**
 * Tests for stream existance.
 */
public final class StreamExistsCommand implements TestCommand<TestContext> {

    // Creation
    // DO NOT CHANGE ORDER OR RENAME VARIABLES!

    private String streamName;

    private boolean shouldExist;

    // Initialization

    private StreamId streamId;

    private EventStore es;

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

    @Override
    public void init(final TestContext context) {
        this.es = context.getEventStore();
        this.streamName = context.getCurrentEventStoreImplType() + "_" + streamName;


        this.streamId = new SimpleStreamId(streamName);

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
        return TestUtils.isExpectedType(null, actualException) && (actuallyExists == shouldExist);
    }

    @Override
    public final String getFailureDescription() {
        if (!TestUtils.isExpectedType(null, actualException)) {
            return EscTestUtils.createExceptionFailureMessage(streamId.asString(), null, actualException);
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
