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

import org.fuin.esc.api.*;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Asynchronous in-memory implementation for unit testing. This implementation is thread-safe: all access to the internal stream and
 * subscription state is guarded by the instance's intrinsic lock. Event notifications are dispatched through the configured {@link Executor}
 * and therefore run outside the critical section. The in-memory operations themselves are cheap, so the returned futures are already
 * completed when handed back; only the subscription event delivery is genuinely asynchronous.
 */
@ThreadSafe
public final class InMemoryEventStoreAsync implements IInMemoryEventStoreAsync {

    private final Executor executor;

    private final Map<String, InternalStream> streams;

    /** Global append-ordered log of every event (across all streams) - the source a projection filters. */
    private final List<CommonEvent> globalLog;

    /** Projection definitions, shared with the {@link InMemoryProjectionAdminEventStore}. */
    private final InMemoryProjections projections;

    private final Map<String, List<InternalSubscription>> subscriptions;

    private volatile boolean open;

    /**
     * Constructor with all mandatory data.
     *
     * @param executor
     *            Executor used to create the necessary threads for event notifications.
     */
    public InMemoryEventStoreAsync(final Executor executor) {
        super();
        Contract.requireArgNotNull("executor", executor);

        this.executor = executor;
        streams = new HashMap<>();
        globalLog = new ArrayList<>();
        projections = new InMemoryProjections();
        subscriptions = new HashMap<>();
        this.open = false;
    }

    /**
     * Returns a projection admin store sharing this event store's projection registry. Projections created,
     * enabled or disabled through it are immediately reflected when reading the corresponding projection stream
     * from this event store (see {@link #readEventsForward(StreamId, long, int)} with a {@code ProjectionStreamId}).
     *
     * @return New projection admin store bound to this event store.
     */
    public ProjectionAdminEventStore getProjectionAdmin() {
        return new InMemoryProjectionAdminEventStore(projections);
    }

    @Override
    public synchronized CompletableFuture<Void> open() {
        this.open = true;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized void close() {
        this.open = false;
    }

    @Override
    public EventStoreCapabilities capabilities() {
        // Volatile store with catch-up/live subscriptions, hard delete and (Java-predicate) projections, but no persistence.
        return EventStoreCapabilities.builder().subscriptions(true).hardDelete(true).projections(true).build();
    }

    @Override
    public boolean isSupportsCreateStream() {
        return false;
    }

    @Override
    public CompletableFuture<Void> createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Do nothing
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> streamExists(final StreamId streamId) {
        return supply(() -> doStreamExists(streamId));
    }

    @Override
    public CompletableFuture<CommonEvent> readEvent(final StreamId streamId, final long eventNumber) {
        return supply(() -> doReadEvent(streamId, eventNumber));
    }

    @Override
    public CompletableFuture<StreamEventsSlice> readEventsForward(final StreamId streamId, final long start, final int count) {
        return supply(() -> doReadEventsForward(streamId, start, count));
    }

    @Override
    public CompletableFuture<StreamEventsSlice> readEventsBackward(final StreamId streamId, final long start, final int count) {
        return supply(() -> doReadEventsBackward(streamId, start, count));
    }

    @Override
    public CompletableFuture<Void> deleteStream(final StreamId streamId, final long expected, final boolean hardDelete) {
        return run(() -> doDeleteStream(streamId, expected, hardDelete));
    }

    @Override
    public CompletableFuture<Void> deleteStream(final StreamId streamId, final boolean hardDelete) {
        return deleteStream(streamId, ExpectedVersion.ANY.getNo(), hardDelete);
    }

    @Override
    public CompletableFuture<Long> appendToStream(final StreamId streamId, final long expectedVersion, final List<CommonEvent> toAppend) {
        return supply(() -> doAppendToStream(streamId, expectedVersion, toAppend));
    }

    @Override
    public CompletableFuture<Long> appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events) {
        return appendToStream(streamId, expectedVersion, Objects.requireNonNull(EscSpiUtils.asList(events)));
    }

    @Override
    public CompletableFuture<Long> appendToStream(final StreamId streamId, final List<CommonEvent> toAppend) {
        return appendToStream(streamId, ExpectedVersion.ANY.getNo(), toAppend);
    }

    @Override
    public CompletableFuture<Long> appendToStream(final StreamId streamId, final CommonEvent... events) {
        Contract.requireArgNotNull("events", events);
        return appendToStream(streamId, Objects.requireNonNull(EscSpiUtils.asList(events)));
    }

    @Override
    public CompletableFuture<Subscription> subscribeToStream(final StreamId streamId, final long eventNumber,
            final BiConsumer<Subscription, CommonEvent> onEvent, final BiConsumer<Subscription, Exception> onDrop) {
        return supply(() -> doSubscribeToStream(streamId, eventNumber, onEvent, onDrop));
    }

    @Override
    public CompletableFuture<Void> unsubscribeFromStream(final Subscription subscription) {
        return run(() -> doUnsubscribeFromStream(subscription));
    }

    @Override
    public CompletableFuture<StreamState> streamState(final StreamId streamId) {
        return supply(() -> doStreamState(streamId));
    }

    private synchronized boolean doStreamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final InternalStream internalStream = streams.get(streamId.asString());
        return (internalStream != null && internalStream.getState() == StreamState.ACTIVE);

    }

    private synchronized CommonEvent doReadEvent(final StreamId streamId, final long eventNumber) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);
        ensureOpen();

        final List<CommonEvent> events = getStream(streamId, ExpectedVersion.ANY.getNo()).getEvents();
        if (events.size() - 1 < eventNumber) {
            throw new EventNotFoundException(streamId, eventNumber);
        }

        return events.get((int) eventNumber);
    }

    private synchronized StreamEventsSlice doReadEventsForward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        if (streamId.isProjection()) {
            return doReadProjectionEventsForward(streamId, start, count);
        }

        final List<CommonEvent> events = getStream(streamId, ExpectedVersion.ANY.getNo()).getEvents();

        final List<CommonEvent> result = new ArrayList<>();
        for (int i = (int) start; (i < (start + count)) && (i < events.size()); i++) {
            result.add(events.get(i));
        }
        final long fromEventNumber = start;
        final long nextEventNumber = (start + result.size());
        final boolean endOfStream = (result.size() < count);

        return new StreamEventsSlice(fromEventNumber, result, nextEventNumber, endOfStream);

    }

    /**
     * Reads a projection stream: filters the global event log with the projection's Java predicate (type name
     * and/or category) and returns a slice, using {@code start} as a zero-based offset into the filtered
     * sequence. Mirrors the JPA backend semantics: an unknown projection throws {@link StreamNotFoundException},
     * a disabled or empty-selection projection returns an empty end-of-stream slice.
     */
    private StreamEventsSlice doReadProjectionEventsForward(final StreamId streamId, final long start, final int count) {

        final InMemoryProjections.InMemoryProjection projection = projections.get(streamId.asString());
        if (projection == null) {
            throw new StreamNotFoundException(streamId);
        }
        if (!projection.isEnabled() || projection.selectsNothing()) {
            return new StreamEventsSlice(start, new ArrayList<>(), start, true);
        }

        final List<CommonEvent> selected = new ArrayList<>();
        for (final CommonEvent event : globalLog) {
            if (projection.matches(event)) {
                selected.add(event);
            }
        }

        final List<CommonEvent> result = new ArrayList<>();
        for (int i = (int) start; (i < (start + count)) && (i < selected.size()); i++) {
            result.add(selected.get(i));
        }
        final long nextEventNumber = (start + result.size());
        final boolean endOfStream = (result.size() < count);

        return new StreamEventsSlice(start, result, nextEventNumber, endOfStream);

    }

    private synchronized StreamEventsSlice doReadEventsBackward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final List<CommonEvent> events = getStream(streamId, ExpectedVersion.ANY.getNo()).getEvents();

        final List<CommonEvent> result = new ArrayList<>();
        if (start < events.size()) {
            for (int i = (int) start; (i > (start - count)) && (i >= 0); i--) {
                result.add(events.get(i));
            }
        }

        final long fromEventNumber = start;
        long nextEventNumber = start - result.size();
        if (nextEventNumber < 0) {
            nextEventNumber = 0;
        }
        final boolean endOfStream = (start - count) < 0;

        return new StreamEventsSlice(fromEventNumber, result, nextEventNumber, endOfStream);
    }

    private synchronized void doDeleteStream(final StreamId streamId, final long expected, final boolean hardDelete) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }

        final InternalStream stream = streams.get(streamId.asString());
        if (stream == null) {
            // Stream never existed
            if (expected == ExpectedVersion.ANY.getNo() || expected == ExpectedVersion.NO_OR_EMPTY_STREAM.getNo()) {
                if (hardDelete) {
                    final InternalStream hds = new InternalStream();
                    hds.delete(hardDelete);
                    streams.put(streamId.asString(), hds);
                }
                // Ignore
                return;
            }
            throw new WrongExpectedVersionException(streamId, expected, null);
        }
        if (stream.getState() == StreamState.SOFT_DELETED) {
            // Ignore
            return;
        }
        if (stream.getState() == StreamState.HARD_DELETED) {
            throw new StreamDeletedException(streamId);
        }
        // StreamState.ACTIVE
        if (expected != ExpectedVersion.ANY.getNo() && expected != stream.getVersion()) {
            throw new WrongExpectedVersionException(streamId, expected, stream.getVersion());
        }
        stream.delete(hardDelete);

    }

    private synchronized long doAppendToStream(final StreamId streamId, final long expectedVersion, final List<CommonEvent> toAppend) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("toAppend", toAppend);
        ensureOpen();

        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }

        InternalStream stream = streams.get(streamId.asString());
        if (stream == null) {
            stream = new InternalStream();
            streams.put(streamId.asString(), stream);
        }
        if (stream.getState() == StreamState.HARD_DELETED) {
            throw new StreamDeletedException(streamId);
        }
        if (stream.getState() == StreamState.SOFT_DELETED) {
            stream.undelete();
        }
        if (expectedVersion != ExpectedVersion.ANY.getNo() && expectedVersion != stream.getVersion()) {
            // Test for idempotency
            final StreamEventsSlice slice = doReadEventsBackward(streamId, stream.getVersion(), toAppend.size());
            final List<CommonEvent> events = slice.getEvents();
            if (EscSpiUtils.eventsEqual(events, toAppend)) {
                return stream.getVersion();
            }
            throw new WrongExpectedVersionException(streamId, expectedVersion, stream.getVersion());
        }

        stream.addAll(toAppend);
        // Keep the global append-ordered log in sync so projections can filter it (same references, no copy).
        globalLog.addAll(toAppend);

        notifyListeners(streamId, toAppend, 0);

        return stream.getVersion();

    }

    private synchronized Subscription doSubscribeToStream(final StreamId streamId, final long eventNumber,
            final BiConsumer<Subscription, CommonEvent> onEvent, final BiConsumer<Subscription, Exception> onDrop) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("onEvent", onEvent);
        Contract.requireArgNotNull("onDrop", onDrop);
        ensureOpen();

        final List<CommonEvent> events = getStream(streamId, ExpectedVersion.ANY.getNo()).getEvents();
        final long lastEventNumber = events.size();
        final int subscriberId = subscriptions.size();

        final InMemorySubscription subscription = new InMemorySubscription(subscriberId, streamId, lastEventNumber);

        List<InternalSubscription> list = subscriptions.get(streamId.asString());
        if (list == null) {
            list = new ArrayList<>();
            subscriptions.put(streamId.asString(), list);
        }
        list.add(new InternalSubscription(subscription, onEvent));

        notifyListeners(streamId, events, eventNumber);

        return subscription;

    }

    private synchronized void doUnsubscribeFromStream(final Subscription subscription) {

        Contract.requireArgNotNull("subscription", subscription);
        ensureOpen();
        if (!(subscription instanceof InMemorySubscription)) {
            throw new IllegalArgumentException(
                    "Can only handle subscriptions of type " + InMemorySubscription.class.getSimpleName() + ", not: ");
        }
        final InMemorySubscription inMemSubscription = (InMemorySubscription) subscription;

        final List<InternalSubscription> list = subscriptions.get(subscription.getStreamId().asString());
        if (list != null) {
            final int idx = indexOf(list, inMemSubscription);
            if (idx > -1) {
                list.remove(idx);
            }
        }

    }

    private synchronized StreamState doStreamState(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final InternalStream stream = streams.get(streamId.asString());
        if (stream == null) {
            throw new StreamNotFoundException(streamId);
        }
        final StreamState state = stream.getState();
        if (state == StreamState.SOFT_DELETED) {
            // TODO Remove after event store has a way to distinguish between
            // never-existing and soft deleted
            // streams
            throw new StreamNotFoundException(streamId);
        }
        return state;
    }

    private void ensureOpen() {
        if (!open) {
            open();
        }
    }

    private void notifyListeners(final StreamId streamId, final List<CommonEvent> events, final long idx) {

        if ((idx > -1) && (idx < events.size())) {

            final List<InternalSubscription> internalSubscriptions = subscriptions.get(streamId.asString());
            if (internalSubscriptions != null) {
                final Iterator<InternalSubscription> it = internalSubscriptions.iterator();
                while (it.hasNext()) {
                    final InternalSubscription internalSubscription = it.next();
                    final BiConsumer<Subscription, CommonEvent> eventListener = internalSubscription.getEventListener();
                    final InMemorySubscription subscription = internalSubscription.getSubscription();
                    final List<CommonEvent> copy = new ArrayList<>(events);
                    if (eventListener != null) {
                        executor.execute(() -> {
                            for (long i = idx; i < copy.size(); i++) {
                                eventListener.accept(subscription, copy.get((int) i));
                            }
                        });
                    }
                }
            }

        }

    }

    private int indexOf(final List<InternalSubscription> list, final InMemorySubscription inMemSubscription) {
        return list.indexOf(new InternalSubscription(inMemSubscription));
    }

    private InternalStream getStream(final StreamId streamId, final long expected) {
        final InternalStream stream = streams.get(streamId.asString());
        if (stream == null) {
            throw new StreamNotFoundException(streamId);
        }
        if (stream.getState() == StreamState.SOFT_DELETED) {
            throw new StreamNotFoundException(streamId);
        }
        if (stream.getState() == StreamState.HARD_DELETED) {
            throw new StreamDeletedException(streamId);
        }
        if (expected != ExpectedVersion.ANY.getNo() && expected != stream.getVersion()) {
            throw new WrongExpectedVersionException(streamId, expected, stream.getVersion());
        }
        return stream;
    }

    private static <T> CompletableFuture<T> supply(final Supplier<T> supplier) {
        try {
            return CompletableFuture.completedFuture(supplier.get());
        } catch (final RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private static CompletableFuture<Void> run(final Runnable runnable) {
        try {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        } catch (final RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    /**
     * A stream.
     */
    private static final class InternalStream {

        private StreamState state;

        private int version;

        private final List<CommonEvent> events;

        /**
         * Deafult constructor.
         */
        public InternalStream() {
            super();
            state = StreamState.ACTIVE;
            version = -1;
            events = new ArrayList<>();
        }

        /**
         * Adds a number of events to the stream.
         *
         * @param events
         *            Events to add.
         */
        public void addAll(final List<CommonEvent> events) {
            this.events.addAll(events);
            version = version + events.size();
        }

        /**
         * Returns the state of the stream.
         *
         * @return State of the stream.
         */
        public StreamState getState() {
            return state;
        }

        /**
         * Current version of the stream.
         *
         * @return Version.
         */
        public long getVersion() {
            return version;
        }

        /**
         * Returns the event list.
         *
         * @return Events before deletion.
         */
        public List<CommonEvent> getEvents() {
            return Collections.unmodifiableList(events);
        }

        /**
         * Hard deletes the stream.
         */
        public void delete(final boolean hardDelete) {
            if (hardDelete) {
                this.state = StreamState.HARD_DELETED;
            } else {
                this.state = StreamState.SOFT_DELETED;
            }
            events.clear();
        }

        /**
         * Reverts the deletion of the stream.
         */
        public void undelete() {
            if (state != StreamState.SOFT_DELETED) {
                throw new IllegalStateException("Undelete impossible, state was: " + state);
            }
            this.state = StreamState.ACTIVE;
        }

    }

    /**
     * Internal structure to store subscriptions and the listeners together.
     */
    private static final class InternalSubscription {

        private final InMemorySubscription subscription;

        @Nullable
        private final BiConsumer<Subscription, CommonEvent> eventListener;

        /**
         * Constructor for find operations.
         *
         * @param subscription
         *            The subscription.
         */
        public InternalSubscription(final InMemorySubscription subscription) {
            this(subscription, null);
        }

        /**
         * Constructor with all mandatory data.
         *
         * @param subscription
         *            The subscription.
         * @param eventListener
         *            Listens to events.
         */
        public InternalSubscription(final InMemorySubscription subscription, @Nullable final BiConsumer<Subscription, CommonEvent> eventListener) {
            super();
            this.subscription = subscription;
            this.eventListener = eventListener;
        }

        @Override
        public int hashCode() {
            return subscription.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof InternalSubscription)) {
                return false;
            }
            final InternalSubscription other = (InternalSubscription) obj;
            return subscription.equals(other.subscription);
        }

        /**
         * Returns the subscription.
         *
         * @return the subscription
         */
        public InMemorySubscription getSubscription() {
            return subscription;
        }

        /**
         * Returns the event listener.
         *
         * @return the listener
         */
        @Nullable
        public BiConsumer<Subscription, CommonEvent> getEventListener() {
            return eventListener;
        }

    }

}
