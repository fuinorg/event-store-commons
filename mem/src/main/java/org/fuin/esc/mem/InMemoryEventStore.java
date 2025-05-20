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

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.Subscription;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.esc.spi.AbstractReadableEventStore;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.objects4j.common.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * In-memory implementation for unit testing. This implementation is **NOT** thread-safe.
 */
public final class InMemoryEventStore extends AbstractReadableEventStore implements IInMemoryEventStore {

    private final Executor executor;

    private final Map<String, InternalStream> streams;

    private final Map<String, List<InternalSubscription>> subscriptions;

    private boolean open;

    /**
     * Constructor with all mandatory data.
     *
     * @param executor
     *            Executor used to create the necessary threads for event notifications.
     */
    public InMemoryEventStore(@NotNull final Executor executor) {
        super();
        Contract.requireArgNotNull("executor", executor);

        this.executor = executor;
        streams = new HashMap<>();
        subscriptions = new HashMap<>();
        this.open = false;
    }

    /**
     * Determines if the store is open.
     *
     * @return {@literal true} in case the {@link #open()} method has been called and no {@link #close()} after that.
     */
    public boolean isOpen() {
        return open;
    }

    @Override
    public InMemoryEventStore open() {
        if (open) {
            // Ignore
            return this;
        }
        this.open = true;
        return this;
    }

    @Override
    public void close() {
        if (!open) {
            // Ignore
            return;
        }
        this.open = false;
    }

    @Override
    public boolean isSupportsCreateStream() {
        return false;
    }

    @Override
    public void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Do nothing
    }

    @Override
    public boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final InternalStream internalStream = streams.get(streamId.asString());
        return (internalStream != null && internalStream.getState() == StreamState.ACTIVE);

    }

    @Override
    public CommonEvent readEvent(final StreamId streamId, final long eventNumber) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);
        ensureOpen();

        final List<CommonEvent> events = getStream(streamId, ExpectedVersion.ANY.getNo()).getEvents();
        if (events.size() - 1 < eventNumber) {
            throw new EventNotFoundException(streamId, eventNumber);
        }

        return events.get((int) eventNumber);
    }

    @Override
    public StreamEventsSlice readEventsForward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

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

    @Override
    public StreamEventsSlice readEventsBackward(final StreamId streamId, final long start, final int count) {

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

    @Override
    public void deleteStream(final StreamId streamId, final long expected, final boolean hardDelete) {

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

    @Override
    public void deleteStream(final StreamId streamId, final boolean hardDelete) {

        deleteStream(streamId, ExpectedVersion.ANY.getNo(), hardDelete);

    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final List<CommonEvent> toAppend) {

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
            final StreamEventsSlice slice = readEventsBackward(streamId, stream.getVersion(), toAppend.size());
            final List<CommonEvent> events = slice.getEvents();
            if (EscSpiUtils.eventsEqual(events, toAppend)) {
                return stream.getVersion();
            }
            throw new WrongExpectedVersionException(streamId, expectedVersion, stream.getVersion());
        }

        stream.addAll(toAppend);

        notifyListeners(streamId, toAppend, 0);

        return stream.getVersion();

    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events) {

        return appendToStream(streamId, expectedVersion, EscSpiUtils.asList(events));

    }

    @Override
    public long appendToStream(final StreamId streamId, final List<CommonEvent> toAppend) {

        return appendToStream(streamId, ExpectedVersion.ANY.getNo(), toAppend);

    }

    @Override
    public long appendToStream(final StreamId streamId, final CommonEvent... events) {

        Contract.requireArgNotNull("events", events);

        return appendToStream(streamId, EscSpiUtils.asList(events));

    }

    @Override
    public Subscription subscribeToStream(final StreamId streamId, final long eventNumber,
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

    @Override
    public void unsubscribeFromStream(final Subscription subscription) {

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

    @Override
    public StreamState streamState(final StreamId streamId) {

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
                    executor.execute(() -> {
                        for (long i = idx; i < copy.size(); i++) {
                            eventListener.accept(subscription, copy.get((int) i));
                        }
                    });
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

        private final BiConsumer<Subscription, CommonEvent> eventListener;

        /**
         * Constructor for find operations. NEVER USE for
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
        public InternalSubscription(final InMemorySubscription subscription, final BiConsumer<Subscription, CommonEvent> eventListener) {
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
        public BiConsumer<Subscription, CommonEvent> getEventListener() {
            return eventListener;
        }

    }

}
