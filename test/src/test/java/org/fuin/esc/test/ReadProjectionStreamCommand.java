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

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.utils4j.TestCommand;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads a projection stream forward and asserts the list of selected event ids (in order). Only the event ids
 * are compared - not the slice metadata (from/next/end-of-stream) - so the same feature works across backends
 * whose projection-stream numbering differs. Because projection processing is asynchronous on some backends
 * (KurrentDB), the read is retried until the expected ids appear or a timeout elapses (the JPA backend is
 * synchronous and matches on the first read).
 */
public final class ReadProjectionStreamCommand implements TestCommand<TestContext> {

    // Generous timeout: KurrentDB projections are eventually consistent, and on a slow/cold CI runner the
    // projection subsystem (RUN_PROJECTIONS=All) needs time to warm up before the first projection emits.
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);

    // Creation (Initialized by Cucumber)

    private String projectionName;

    private long start;

    private int count;

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

    private StreamId projectionStreamId;

    private EventStore es;

    private List<String> expectedIds;

    // Execution

    private List<String> actualIds;

    private volatile Throwable lastReadError;

    /**
     * Default constructor used by Cucumber.
     */
    public ReadProjectionStreamCommand() {
        super();
    }

    /**
     * Creates an instance with values from the table headers in the feature.
     *
     * @param cucumberTable Column values.
     */
    public ReadProjectionStreamCommand(final Map<String, String> cucumberTable) {
        this.projectionName = cucumberTable.get("Projection Name");
        this.start = Long.parseLong(cucumberTable.get("Start"));
        this.count = Integer.parseInt(cucumberTable.get("Count"));
        this.resultEventId1 = cucumberTable.get("Result Event Id 1");
        this.resultEventId2 = cucumberTable.get("Result Event Id 2");
        this.resultEventId3 = cucumberTable.get("Result Event Id 3");
        this.resultEventId4 = cucumberTable.get("Result Event Id 4");
        this.resultEventId5 = cucumberTable.get("Result Event Id 5");
        this.resultEventId6 = cucumberTable.get("Result Event Id 6");
        this.resultEventId7 = cucumberTable.get("Result Event Id 7");
        this.resultEventId8 = cucumberTable.get("Result Event Id 8");
        this.resultEventId9 = cucumberTable.get("Result Event Id 9");
    }

    @Override
    public void init(final TestContext context) {
        this.es = context.getEventStore();
        this.projectionName = context.getCurrentEventStoreImplType() + "_" + projectionName;

        resultEventId1 = EscTestUtils.emptyAsNull(resultEventId1);
        resultEventId2 = EscTestUtils.emptyAsNull(resultEventId2);
        resultEventId3 = EscTestUtils.emptyAsNull(resultEventId3);
        resultEventId4 = EscTestUtils.emptyAsNull(resultEventId4);
        resultEventId5 = EscTestUtils.emptyAsNull(resultEventId5);
        resultEventId6 = EscTestUtils.emptyAsNull(resultEventId6);
        resultEventId7 = EscTestUtils.emptyAsNull(resultEventId7);
        resultEventId8 = EscTestUtils.emptyAsNull(resultEventId8);
        resultEventId9 = EscTestUtils.emptyAsNull(resultEventId9);

        projectionStreamId = new ProjectionStreamId(projectionName);
        expectedIds = new ArrayList<>();
        addId(expectedIds, resultEventId1);
        addId(expectedIds, resultEventId2);
        addId(expectedIds, resultEventId3);
        addId(expectedIds, resultEventId4);
        addId(expectedIds, resultEventId5);
        addId(expectedIds, resultEventId6);
        addId(expectedIds, resultEventId7);
        addId(expectedIds, resultEventId8);
        addId(expectedIds, resultEventId9);
    }

    private static void addId(final List<String> ids, final String eventId) {
        if (eventId != null) {
            ids.add(eventId);
        }
    }

    private List<String> readIds() {
        final StreamEventsSlice slice = es.readEventsForward(projectionStreamId, start, count);
        final List<String> ids = new ArrayList<>();
        for (final CommonEvent event : slice.getEvents()) {
            ids.add(event.getId().asBaseType().toString());
        }
        return ids;
    }

    @Override
    public void execute() {
        try {
            // Retry until the projection has caught up (KurrentDB is eventual; JPA/mem match immediately).
            // Exceptions (e.g. StreamNotFoundException while the projection stream is not emitted yet) are
            // captured instead of silently ignored, so a real timeout reports the last cause.
            Awaitility.await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).until(() -> {
                try {
                    actualIds = readIds();
                    lastReadError = null;
                    return expectedIds.equals(actualIds);
                } catch (final RuntimeException ex) {
                    lastReadError = ex;
                    return false;
                }
            });
        } catch (final ConditionTimeoutException ex) {
            // Not caught up in time - verify() reports the mismatch (and last read error) via getFailureDescription.
        }
    }

    @Override
    public boolean isSuccessful() {
        return expectedIds.equals(actualIds);
    }

    @Override
    public String getFailureDescription() {
        final String cause = (actualIds == null && lastReadError != null) ? " (last read error: " + lastReadError + ")" : "";
        return "[" + projectionStreamId + "] expected event ids " + expectedIds + ", but was: " + actualIds + cause;
    }

    @Override
    public void verify() {
        if (!isSuccessful()) {
            throw new RuntimeException(getFailureDescription());
        }
    }

    @Override
    public String toString() {
        return "ReadProjectionStreamCommand [projectionName=" + projectionName + ", start=" + start + ", count="
                + count + ", expectedIds=" + expectedIds + ", actualIds=" + actualIds + "]";
    }

}
