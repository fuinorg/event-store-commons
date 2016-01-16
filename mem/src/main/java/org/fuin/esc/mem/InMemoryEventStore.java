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
package org.fuin.esc.mem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.SubscribableEventStore;
import org.fuin.esc.api.Subscription;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.objects4j.common.Contract;

/**
 * In-memory implementation for unit testing. This implementation is **NOT** thread-safe.
 */
public final class InMemoryEventStore implements EventStore, SubscribableEventStore {

    private Executor executor;

    private List<CommonEvent> all;

    private Map<StreamId, InternalStream> streams;

    private Map<StreamId, List<InternalSubscription>> subscriptions;

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
        all = new ArrayList<CommonEvent>();
        streams = new HashMap<>();
        subscriptions = new HashMap<>();
    }

    @Override
    public final void open() {
        // Do nothing
    }

    @Override
    public final void close() {
        // Do nothing
    }

    @Override
    public final void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Do nothing
    }

    @Override
    public final boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);

        final InternalStream internalStream = streams.get(streamId);
        return (internalStream != null && internalStream.getState() == StreamState.ACTIVE);

    }

    @Override
    public final CommonEvent readEvent(final StreamId streamId, final int eventNumber) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);

        final List<CommonEvent> events = getStream(streamId, ExpectedVersion.ANY.getNo()).getEvents();
        if (events.size() - 1 < eventNumber) {
            throw new EventNotFoundException(streamId, eventNumber);
        }

        return events.get(eventNumber);
    }

    @Override
    public final StreamEventsSlice readEventsForward(final StreamId streamId, final int start, 
            final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);

        final List<CommonEvent> events;
        if (streamId == StreamId.ALL) {
            events = all;
        } else {
            events = getStream(streamId, ExpectedVersion.ANY.getNo()).getEvents();
        }

        final List<CommonEvent> result = new ArrayList<CommonEvent>();
        for (int i = start; (i < (start + count)) && (i < events.size()); i++) {
            result.add(events.get(i));
        }
        final int fromEventNumber = start;
        final int nextEventNumber = (start + result.size());
        final boolean endOfStream = (result.size() < count);

        return new StreamEventsSlice(fromEventNumber, result, nextEventNumber, endOfStream);

    }

    @Override
    public final StreamEventsSlice readEventsBackward(final StreamId streamId, final int start,
            final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);

        final List<CommonEvent> events;
        if (streamId == StreamId.ALL) {
            events = all;
        } else {
            events = getStream(streamId, ExpectedVersion.ANY.getNo()).getEvents();
        }

        final List<CommonEvent> result = new ArrayList<CommonEvent>();
        if (start < events.size()) {
            for (int i = start; (i > (start - count)) && (i >= 0); i--) {
                result.add(events.get(i));
            }
        }

        final int fromEventNumber = start;
        int nextEventNumber = start - result.size();
        if (nextEventNumber < 0) {
            nextEventNumber = 0;
        }
        final boolean endOfStream = (start - count) < 0;

        return new StreamEventsSlice(fromEventNumber, result, nextEventNumber, endOfStream);
    }

    @Override
    public final void deleteStream(final StreamId streamId, final int expected, final boolean hardDelete) {

        Contract.requireArgNotNull("streamId", streamId);

        if (streamId == StreamId.ALL) {
            throw new IllegalArgumentException("It's not possible to delete the 'all' stream");
        }

        final InternalStream stream = streams.get(streamId);
        if (stream == null) {
            // Stream never existed
            if (expected == ExpectedVersion.ANY.getNo()
                    || expected == ExpectedVersion.NO_OR_EMPTY_STREAM.getNo()) {
                if (hardDelete) {
                    final InternalStream hds = new InternalStream();
                    hds.delete(hardDelete);
                    streams.put(streamId, hds);
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
    public final void deleteStream(final StreamId streamId, final boolean hardDelete) {

        deleteStream(streamId, ExpectedVersion.ANY.getNo(), hardDelete);

    }

    @Override
    public final int appendToStream(final StreamId streamId, final int expectedVersion,
            final List<CommonEvent> toAppend) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("toAppend", toAppend);

        InternalStream stream = streams.get(streamId);
        if (stream == null) {
            stream = new InternalStream();
            streams.put(streamId, stream);
        }
        if (stream.getState() == StreamState.HARD_DELETED) {
            throw new StreamDeletedException(streamId);
        }
        if (stream.getState() == StreamState.SOFT_DELETED) {
            stream.undelete();
        }
        if (expectedVersion != ExpectedVersion.ANY.getNo() && expectedVersion != stream.getVersion()) {
            final List<CommonEvent> events = stream.getEvents();
            if (EscSpiUtils.eventsEqual(events, toAppend)) {
                return stream.getVersion();
            }
            throw new WrongExpectedVersionException(streamId, expectedVersion, stream.getVersion());
        }

        all.addAll(toAppend);
        stream.addAll(toAppend);

        notifyListeners(streamId, toAppend, 0);

        return stream.getVersion();

    }

    @Override
    public final int appendToStream(final StreamId streamId, final int expectedVersion,
            final CommonEvent... events) {

        return appendToStream(streamId, expectedVersion, EscSpiUtils.asList(events));

    }

    @Override
    public final int appendToStream(final StreamId streamId, final List<CommonEvent> toAppend) {

        return appendToStream(streamId, ExpectedVersion.ANY.getNo(), toAppend);

    }

    @Override
    public final int appendToStream(final StreamId streamId, final CommonEvent... events) {

        Contract.requireArgNotNull("events", events);

        return appendToStream(streamId, EscSpiUtils.asList(events));

    }

    @Override
    public final Subscription subscribeToStream(final StreamId streamId, final int eventNumber,
            final BiConsumer<Subscription, CommonEvent> onEvent,
            final BiConsumer<Subscription, Exception> onDrop) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("onEvent", onEvent);
        Contract.requireArgNotNull("onDrop", onDrop);

        final List<CommonEvent> events = getStream(streamId, ExpectedVersion.ANY.getNo()).getEvents();
        final Integer lastEventNumber = events.size();
        final int subscriberId = subscriptions.size();

        final InMemorySubscription subscription = new InMemorySubscription(subscriberId, streamId,
                lastEventNumber);

        List<InternalSubscription> list = subscriptions.get(streamId);
        if (list == null) {
            list = new ArrayList<>();
            subscriptions.put(streamId, list);
        }
        list.add(new InternalSubscription(subscription, onEvent));

        notifyListeners(streamId, events, eventNumber);

        return subscription;

    }

    @Override
    public final void unsubscribeFromStream(final Subscription subscription) {

        Contract.requireArgNotNull("subscription", subscription);
        if (!(subscription instanceof InMemorySubscription)) {
            throw new IllegalArgumentException("Can only handle subscriptions of type "
                    + InMemorySubscription.class.getSimpleName() + ", not: ");
        }
        final InMemorySubscription inMemSubscription = (InMemorySubscription) subscription;

        final List<InternalSubscription> list = subscriptions.get(subscription.getStreamId());
        if (list != null) {
            final int idx = indexOf(list, inMemSubscription);
            if (idx > -1) {
                list.remove(idx);
            }
        }

    }

    @Override
    public final StreamState streamState(final StreamId streamId) {
        final InternalStream stream = streams.get(streamId);
        if (stream == null) {
            throw new StreamNotFoundException(streamId);
        }
        final StreamState state = stream.getState();
        if (state == StreamState.SOFT_DELETED) {
            // TODO Remove after event store has a way to distinguish between never-existing and soft deleted
            // streams
            throw new StreamNotFoundException(streamId);
        }
        return state;
    }

    private void notifyListeners(final StreamId streamId, final List<CommonEvent> events, final int idx) {

        if ((idx > -1) && (idx < events.size())) {

            final List<InternalSubscription> internalSubscriptions = subscriptions.get(streamId);
            if (internalSubscriptions != null) {
                final Iterator<InternalSubscription> it = internalSubscriptions.iterator();
                while (it.hasNext()) {
                    final InternalSubscription internalSubscription = it.next();
                    final BiConsumer<Subscription, CommonEvent> eventListener = internalSubscription
                            .getEventListener();
                    final InMemorySubscription subscription = internalSubscription.getSubscription();
                    final List<CommonEvent> copy = new ArrayList<>(events);
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = idx; i < copy.size(); i++) {
                                eventListener.accept(subscription, copy.get(i));
                            }
                        }
                    });
                }
            }

        }

    }

    private int indexOf(final List<InternalSubscription> list, final InMemorySubscription inMemSubscription) {
        return list.indexOf(new InternalSubscription(inMemSubscription));
    }

    private InternalStream getStream(final StreamId streamId, final int expected) {
        final InternalStream stream = streams.get(streamId);
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
        public final void addAll(final List<CommonEvent> events) {
            this.events.addAll(events);
            version = version + events.size();
        }

        /**
         * Returns the state of the stream.
         * 
         * @return TRUE if it was a hard delete.
         */
        public final StreamState getState() {
            return state;
        }

        /**
         * Current version of the stream.
         * 
         * @return Version.
         */
        public final int getVersion() {
            return version;
        }

        /**
         * Returns the event list.
         * 
         * @return Events before deletion.
         */
        public final List<CommonEvent> getEvents() {
            return Collections.unmodifiableList(events);
        }

        /**
         * Hard deletes the stream.
         */
        public final void delete(final boolean hardDelete) {
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
        public final void undelete() {
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
         * @param dropListener
         *            Listens to exceptions.
         */
        public InternalSubscription(final InMemorySubscription subscription,
                final BiConsumer<Subscription, CommonEvent> eventListener) {
            super();
            this.subscription = subscription;
            this.eventListener = eventListener;
        }

        @Override
        public final int hashCode() {
            return subscription.hashCode();
        }

        @Override
        public final boolean equals(final Object obj) {
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
        public final InMemorySubscription getSubscription() {
            return subscription;
        }

        /**
         * Returns the event listener.
         * 
         * @return the listener
         */
        public final BiConsumer<Subscription, CommonEvent> getEventListener() {
            return eventListener;
        }

    }

}
