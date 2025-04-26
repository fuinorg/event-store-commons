/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.test;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.utils4j.TestCommand;

import java.util.Map;

/**
 * Reads a stream backward and expects and exception.
 */
public final class ReadBackwardExceptionCommand implements TestCommand<TestContext> {

    // Creation (Initialized by Cucumber)

    private String streamName;

    private long start;

    private int count;

    private String expectedException;

    private String expectedMessage;

    // Initialization

    private StreamId streamId;

    private EventStore es;

    private Class<? extends Exception> expectedExceptionClass;

    // Execution

    private Exception actualException;

    /**
     * Default constructor used by Cucumber.
     */
    public ReadBackwardExceptionCommand() {
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
     * @param expectedMessage
     *            The exception message that is expected, an empty string or "-".
     */
    public ReadBackwardExceptionCommand(@NotNull final String streamName, final long start, final int count,
                                        final String expectedException, final String expectedMessage) {
        super();
        this.streamName = streamName;
        this.start = start;
        this.count = count;
        this.expectedException = expectedException;
        this.expectedMessage = expectedMessage;
    }

    /**
     * Creates an instance with values from the table headers in the feature.
     *
     * @param cucumberTable Column values.
     */
    public ReadBackwardExceptionCommand(Map<String, String> cucumberTable) {
        this.streamName = cucumberTable.get("Stream Name");
        this.start = Long.parseLong(cucumberTable.get("Start"));
        this.count = Integer.parseInt(cucumberTable.get("Count"));
        this.expectedException = cucumberTable.get("Expected Exception");
        this.expectedMessage = cucumberTable.get("Expected Message");
    }

    @Override
    public void init(final TestContext context) {
        this.es = context.getEventStore();
        this.streamName = context.getCurrentEventStoreImplType() + "_" + streamName;

        expectedException = EscTestUtils.emptyAsNull(expectedException);
        expectedMessage = EscTestUtils.emptyAsNull(expectedMessage);

        streamId = new SimpleStreamId(streamName);
        expectedExceptionClass = EscTestUtils.exceptionForName(expectedException);

    }

    @Override
    public final void execute() {
        try {
            es.readEventsBackward(streamId, start, count);
        } catch (final Exception ex) {
            this.actualException = ex;
        }
    }

    @Override
    public final boolean isSuccessful() {
        return TestUtils.isExpectedException(expectedExceptionClass, expectedMessage, actualException);
    }

    @Override
    public final String getFailureDescription() {
        return EscTestUtils.createExceptionFailureMessage(streamId.asString(), expectedExceptionClass, expectedMessage,
                actualException);
    }

    @Override
    public final void verify() {
        if (!isSuccessful()) {
            throw new RuntimeException(getFailureDescription());
        }
    }

    @Override
    public final String toString() {
        return "ReadBackwardExceptionCommand [streamName=" + streamName + ", start=" + start + ", count="
                + count + ", expectedException=" + expectedException + ", actualException=" + actualException
                + "]";
    }

}
