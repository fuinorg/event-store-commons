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

import org.fuin.esc.api.CheckpointStore;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.ReadableEventStore;
import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Backend-neutral catch-up subscription: repeatedly reads a stream forward from the position stored in a
 * {@link CheckpointStore} and dispatches each chunk to a listener, advancing the checkpoint after each chunk.
 * Passes are triggered by a {@link WakeupSource} (portable interval poll by default) and never overlap.
 * <p>
 * This hoists the checkpoint + poll pattern that upper layers (e.g. {@code cqrs-4-java}'s view manager)
 * re-implement, so a relational or in-memory backend can drive read models without EventStoreDB.
 * <p>
 * <b>Delivery is at-least-once.</b> The checkpoint is advanced only after the chunk listener returns; if the
 * listener throws, the pass stops and the chunk is redelivered on the next wake-up. To make dispatch and
 * checkpoint advance atomic, back the {@link CheckpointStore} with the same transaction the listener uses.
 */
@ThreadSafe
public final class CatchupSubscription implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(CatchupSubscription.class);

    /**
     * Receives a chunk of events during a catch-up pass.
     */
    @FunctionalInterface
    public interface Listener {

        /**
         * Handles a chunk of events. Any exception stops the current pass and leaves the checkpoint at the
         * position before the chunk (the chunk is redelivered next pass).
         *
         * @param events Events to handle (never {@literal null}, never empty).
         */
        void onEvents(List<CommonEvent> events);
    }

    private final ReadableEventStore eventStore;

    private final StreamId streamId;

    private final CheckpointStore checkpoints;

    private final int chunkSize;

    private final Listener listener;

    private final WakeupSource wakeupSource;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Constructor with all mandatory data.
     *
     * @param eventStore   Event store to read from.
     * @param streamId     Stream to follow.
     * @param checkpoints  Store holding the durable read position.
     * @param chunkSize    Number of events read per round trip (must be &gt;= 1).
     * @param listener     Receives each chunk of events.
     * @param wakeupSource Triggers catch-up passes.
     */
    public CatchupSubscription(final ReadableEventStore eventStore, final StreamId streamId,
                               final CheckpointStore checkpoints, final int chunkSize, final Listener listener,
                               final WakeupSource wakeupSource) {
        super();
        Contract.requireArgNotNull("eventStore", eventStore);
        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("checkpoints", checkpoints);
        Contract.requireArgMin("chunkSize", chunkSize, 1);
        Contract.requireArgNotNull("listener", listener);
        Contract.requireArgNotNull("wakeupSource", wakeupSource);
        this.eventStore = eventStore;
        this.streamId = streamId;
        this.checkpoints = checkpoints;
        this.chunkSize = chunkSize;
        this.listener = listener;
        this.wakeupSource = wakeupSource;
    }

    /**
     * Starts the subscription: wires the wake-up source to run guarded catch-up passes.
     */
    public void start() {
        wakeupSource.start(this::catchUpGuarded);
    }

    /**
     * Runs a single catch-up pass unless one is already in progress (in which case it is skipped) and never
     * lets an exception escape to the wake-up source (which could otherwise cancel further passes). This is
     * the method wired to the {@link WakeupSource}.
     */
    public void catchUpGuarded() {
        if (!running.compareAndSet(false, true)) {
            LOG.trace("Catch-up already in progress for stream {}, skipping", streamId.asString());
            return;
        }
        try {
            catchUp();
        } catch (final RuntimeException ex) {
            LOG.error("Error during catch-up for stream {}", streamId.asString(), ex);
        } finally {
            running.set(false);
        }
    }

    /**
     * Reads the stream forward from the stored checkpoint to its head, dispatching each chunk and advancing
     * the checkpoint after each one. Propagates any exception thrown by the listener.
     */
    public void catchUp() {
        final long from = checkpoints.readCheckpoint(streamId);
        eventStore.readAllEventsForward(streamId, from, chunkSize, currentSlice -> {
            listener.onEvents(currentSlice.getEvents());
            checkpoints.updateCheckpoint(streamId, currentSlice.getNextEventNumber());
        });
    }

    @Override
    public void close() {
        wakeupSource.close();
    }

}
