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
package org.fuin.esc.spi;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.TypeName;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link CatchupSubscription} class against a fake event store.
 */
public class CatchupSubscriptionTest {

    private static final StreamId STREAM_ID = new SimpleStreamId("MyStream");

    @Test
    public void testCatchUpDeliversAllAndAdvancesCheckpoint() {

        // PREPARE: 3 events, chunk size 2 -> two chunks
        final FakeEventStore es = new FakeEventStore();
        es.append(event("1"), event("2"), event("3"));
        final InMemoryCheckpointStore checkpoints = new InMemoryCheckpointStore();
        final List<CommonEvent> received = new ArrayList<>();
        final CatchupSubscription testee = new CatchupSubscription(es, STREAM_ID, checkpoints, 2, received::addAll,
                new CapturingWakeupSource());

        // TEST
        testee.catchUp();

        // VERIFY
        assertThat(received).hasSize(3);
        assertThat(checkpoints.readCheckpoint(STREAM_ID)).isEqualTo(3);

    }

    @Test
    public void testIncrementalDeliveryDoesNotRedeliver() {

        // PREPARE
        final FakeEventStore es = new FakeEventStore();
        es.append(event("1"), event("2"), event("3"));
        final InMemoryCheckpointStore checkpoints = new InMemoryCheckpointStore();
        final List<CommonEvent> received = new ArrayList<>();
        final CatchupSubscription testee = new CatchupSubscription(es, STREAM_ID, checkpoints, 10, received::addAll,
                new CapturingWakeupSource());
        testee.catchUp();
        received.clear();

        // TEST: append 2 more and catch up again
        es.append(event("4"), event("5"));
        testee.catchUp();

        // VERIFY: only the 2 new events, checkpoint at 5
        assertThat(received).hasSize(2);
        assertThat(checkpoints.readCheckpoint(STREAM_ID)).isEqualTo(5);

    }

    @Test
    public void testListenerFailureLeavesCheckpointUnadvanced() {

        // PREPARE
        final FakeEventStore es = new FakeEventStore();
        es.append(event("1"), event("2"));
        final InMemoryCheckpointStore checkpoints = new InMemoryCheckpointStore();
        final CatchupSubscription testee = new CatchupSubscription(es, STREAM_ID, checkpoints, 10, events -> {
            throw new IllegalStateException("boom");
        }, new CapturingWakeupSource());

        // TEST & VERIFY: the exception propagates and the checkpoint stays at the beginning
        assertThatThrownBy(testee::catchUp).isInstanceOf(IllegalStateException.class).hasMessage("boom");
        assertThat(checkpoints.readCheckpoint(STREAM_ID)).isZero();

    }

    @Test
    public void testCatchUpGuardedSwallowsException() {

        // PREPARE
        final FakeEventStore es = new FakeEventStore();
        es.append(event("1"));
        final InMemoryCheckpointStore checkpoints = new InMemoryCheckpointStore();
        final CatchupSubscription testee = new CatchupSubscription(es, STREAM_ID, checkpoints, 10, events -> {
            throw new IllegalStateException("boom");
        }, new CapturingWakeupSource());

        // TEST: guarded version must not propagate (so the poll loop keeps running)
        testee.catchUpGuarded();

        // VERIFY
        assertThat(checkpoints.readCheckpoint(STREAM_ID)).isZero();

    }

    @Test
    public void testStartWiresWakeupSource() {

        // PREPARE
        final FakeEventStore es = new FakeEventStore();
        es.append(event("1"));
        final InMemoryCheckpointStore checkpoints = new InMemoryCheckpointStore();
        final List<CommonEvent> received = new ArrayList<>();
        final CapturingWakeupSource wakeup = new CapturingWakeupSource();
        final CatchupSubscription testee = new CatchupSubscription(es, STREAM_ID, checkpoints, 10, received::addAll,
                wakeup);

        // TEST
        testee.start();
        assertThat(wakeup.captured).isNotNull();
        wakeup.captured.run(); // simulate a wake-up

        // VERIFY
        assertThat(received).hasSize(1);
        testee.close();
        assertThat(wakeup.closed).isTrue();

    }

    private static CommonEvent event(final String data) {
        return new SimpleCommonEvent(new EventId(), new TypeName("MyEvent"), data, null);
    }

    /**
     * Minimal {@link org.fuin.esc.api.ReadableEventStore} over an in-memory list; only forward reads matter.
     */
    private static final class FakeEventStore extends AbstractReadableEventStore {

        private final List<CommonEvent> events = new ArrayList<>();

        void append(final CommonEvent... toAppend) {
            for (final CommonEvent e : toAppend) {
                events.add(e);
            }
        }

        @Override
        public StreamEventsSlice readEventsForward(final StreamId streamId, final long start, final int count) {
            final List<CommonEvent> result = new ArrayList<>();
            for (int i = (int) start; (i < start + count) && (i < events.size()); i++) {
                result.add(events.get(i));
            }
            final long next = start + result.size();
            final boolean endOfStream = result.size() < count;
            return new StreamEventsSlice(start, result, next, endOfStream);
        }

        @Override
        public StreamEventsSlice readEventsBackward(final StreamId streamId, final long start, final int count) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CommonEvent readEvent(final StreamId streamId, final long eventNumber) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean streamExists(final StreamId streamId) {
            return true;
        }

        @Override
        public StreamState streamState(final StreamId streamId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FakeEventStore open() {
            return this;
        }

        @Override
        public void close() {
            // Nothing to do
        }

    }

    /**
     * {@link WakeupSource} that just captures the callback so the test can trigger it manually.
     */
    private static final class CapturingWakeupSource implements WakeupSource {

        @Nullable
        private Runnable captured;

        private boolean closed;

        @Override
        public void start(final Runnable onWakeup) {
            this.captured = onWakeup;
        }

        @Override
        public void close() {
            this.closed = true;
        }

    }

}
