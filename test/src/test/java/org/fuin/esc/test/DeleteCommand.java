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

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.units4j.TestCommand;
import org.fuin.units4j.Units4JUtils;

/**
 * Deletes a stream.
 */
public final class DeleteCommand implements TestCommand<TestContext> {

    // Creation (Initialized by Cucumber)
    // DO NOT CHANGE ORDER OR RENAME VARIABLES!

    private String streamName;

    private boolean hardDelete;

    private String expectedVersion;

    private String expectedException;

    // Initialization

    private EventStore es;

    private StreamId streamId;

    private long expectedIntVersion;

    private Class<? extends Exception> expectedExceptionClass;

    // Execution

    private Exception actualException;

    /**
     * Default constructor used by Cucumber.
     */
    public DeleteCommand() {
        super();
    }

    /**
     * Constructor for manual creation.
     * 
     * @param streamName
     *            Uniquely identifies the stream to create.
     * @param hardDelete
     *            TRUE if it should be impossible to recreate the stream. FALSE (soft delete) if appending to
     *            it will recreate it. Please note that in this case the version numbers do not start at zero
     *            but at where you previously soft deleted the stream from.
     * @param expectedVersion
     *            The version the stream should have when being deleted.
     * @param expectedException
     *            The exception that is expected, an empty string or "-".
     */
    public DeleteCommand(@NotNull final String streamName, final boolean hardDelete,
            final String expectedVersion, final String expectedException) {
        super();
        this.streamName = streamName;
        this.hardDelete = hardDelete;
        this.expectedVersion = expectedVersion;
        this.expectedException = expectedException;
    }

    @Override
    public void init(final TestContext context) {
        this.es = context.getEventStore();
        this.streamName = context.getCurrentEventStoreImplType() + "_" + streamName;

        expectedVersion = EscTestUtils.emptyAsNull(expectedVersion);
        expectedException = EscTestUtils.emptyAsNull(expectedException);

        streamId = new SimpleStreamId(streamName);
        expectedIntVersion = ExpectedVersion.no(expectedVersion);
        expectedExceptionClass = EscTestUtils.exceptionForName(expectedException);

    }

    @Override
    public void execute() {
        try {
            es.deleteStream(streamId, expectedIntVersion, hardDelete);
        } catch (final Exception ex) {
            actualException = ex;
        }
    }

    @Override
    public final boolean isSuccessful() {
        return Units4JUtils.isExpectedType(expectedExceptionClass, actualException);
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
        return "DeleteCommand [streamName=" + streamName + ", hardDelete=" + hardDelete
                + ", expectedVersion=" + expectedVersion + ", expectedException="
                + expectedException + ", actualException=" + actualException + "]";
    }

}
