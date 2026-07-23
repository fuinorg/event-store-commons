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
package org.fuin.esc.mem;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EscConnectionException;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the idempotency contract a caller relies on when it retries an append after an
 * {@link EscConnectionException}: the failure leaves the outcome unknown, so the retry may hit a store that
 * already applied the write. A concrete expected version is what makes that retry safe -
 * {@link ExpectedVersion#ANY} appends the events a second time.
 * <p>
 * How the store reports the rejected repetition is backend specific and deliberately not asserted here: this
 * in-memory backend recognizes that the tail of the stream already holds exactly those events and answers
 * with the current version (a silent no-op), while KurrentDB answers with a
 * {@link WrongExpectedVersionException}. Both keep the stream free of duplicates, which is the invariant a
 * retrying caller depends on; a caller that must tell "already applied" from "conflicting write" has to
 * handle both answers.
 */
public class AppendRetryIdempotencyTest {

    private static final StreamId STREAM = new SimpleStreamId("MyStream");

    private InMemoryEventStore testee;

    @BeforeEach
    public void setup() {
        testee = new InMemoryEventStore(Executors.newCachedThreadPool());
        testee.open();
    }

    @AfterEach
    public void teardown() {
        testee.close();
        testee = null;
    }

    @Test
    public void testRetryWithExpectedVersionDoesNotAppendTwice() {

        // PREPARE: the append reached the store, but the caller only saw a connection failure
        final CommonEvent event = event("one");
        testee.appendToStream(STREAM, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event);

        // TEST: the caller retries with the same expected version
        testee.appendToStream(STREAM, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event);

        // VERIFY: the repetition did not get through - the stream still holds exactly one event
        assertThat(readAll()).containsExactly(event);
    }

    @Test
    public void testRetryOfALaterAppendDoesNotAppendTwice() {

        // PREPARE: a stream that already has events, so the expected version is not the "empty" special case
        testee.appendToStream(STREAM, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event("one"), event("two"));
        final CommonEvent third = event("three");
        testee.appendToStream(STREAM, 1, third);

        // TEST
        testee.appendToStream(STREAM, 1, third);

        // VERIFY
        assertThat(readAll()).hasSize(3);
    }

    @Test
    public void testAConflictingAppendIsStillRejected() {

        // PREPARE: the deduplication must not swallow a genuine concurrency conflict
        testee.appendToStream(STREAM, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event("one"));

        // TEST: someone else wrote in the meantime, so the expected version is stale - and the events differ
        assertThatThrownBy(() -> testee.appendToStream(STREAM, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                event("something else"))).isInstanceOf(WrongExpectedVersionException.class);

        // VERIFY
        assertThat(readAll()).hasSize(1);
    }

    @Test
    public void testRetryWithExpectedVersionAnyDuplicatesTheEvents() {

        // PREPARE
        final CommonEvent event = event("one");
        testee.appendToStream(STREAM, ExpectedVersion.ANY.getNo(), event);

        // TEST: this is what a naive retry does - the store has no way to recognize the repetition
        testee.appendToStream(STREAM, ExpectedVersion.ANY.getNo(), event);

        // VERIFY: the duplicate is why a retry needs an expected version or a deduplication mechanism
        assertThat(readAll()).containsExactly(event, event);
    }

    private java.util.List<CommonEvent> readAll() {
        final StreamEventsSlice slice = testee.readEventsForward(STREAM, 0, 100);
        return slice.getEvents();
    }

    private static CommonEvent event(final String data) {
        return new SimpleCommonEvent(new EventId(), new TypeName("MyEvent"), data, null);
    }

}
