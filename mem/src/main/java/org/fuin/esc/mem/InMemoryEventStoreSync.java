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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamVersionConflictException;
import org.fuin.esc.api.SubscribableEventStoreSync;
import org.fuin.esc.api.Subscription;
import org.fuin.objects4j.common.Contract;

/**
 * In-memory implementation for unit testing. This implementation is **NOT** thread-safe.
 */
public final class InMemoryEventStoreSync implements EventStoreSync, SubscribableEventStoreSync {

    private Executor executor;

    private List<CommonEvent> all;

    private Map<StreamId, List<CommonEvent>> streams;

    private Map<StreamId, List<CommonEvent>> deletedStreams;

    private Map<StreamId, List<InternalSubscription>> subscriptions;

    /**
     * Constructor with all mandatory data.
     * 
     * @param executor
     *            Executor used to create the necessary threads for event notifications.
     */
    public InMemoryEventStoreSync(@NotNull final Executor executor) {
        super();
        Contract.requireArgNotNull("executor", executor);

        this.executor = executor;
        all = new ArrayList<CommonEvent>();
        streams = new HashMap<>();
        deletedStreams = new HashMap<>();
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
    public final boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);

        return (streams.get(streamId) != null);

    }

    @Override
    public final CommonEvent readEvent(final StreamId streamId, final int eventNumber) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);

        final List<CommonEvent> events = getStream(streamId);
        if (events.size() - 1 < eventNumber) {
            throw new EventNotFoundException(streamId, eventNumber);
        }

        return events.get(eventNumber);
    }

    @Override
    public final StreamEventsSlice readEventsForward(final StreamId streamId, final int start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);

        final List<CommonEvent> events;
        if (streamId == StreamId.ALL) {
            events = all;
        } else {
            events = getStream(streamId);
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
            events = getStream(streamId);
        }

        final List<CommonEvent> result = new ArrayList<CommonEvent>();
        if (start < events.size()) {
            for (int i = start; (i > (start - count)) && (i >= 0); i--) {
                result.add(events.get(i));
            }
        }

        final int fromEventNumber = start;
        final int nextEventNumber = start - result.size();
        final boolean endOfStream = (start - count) < 0;

        return new StreamEventsSlice(fromEventNumber, result, nextEventNumber, endOfStream);
    }

    @Override
    public final void deleteStream(final StreamId streamId, final int expected, final boolean hardDelete) {

        Contract.requireArgNotNull("streamId", streamId);

        // TODO Handle hard delete

        if (streamId == StreamId.ALL) {
            throw new IllegalArgumentException("It's not possible to delete the 'all' stream");
        }

        final List<CommonEvent> events = getStream(streamId, expected);
        deletedStreams.put(streamId, events);
        streams.remove(streamId);

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

        final List<CommonEvent> events = createIfNotExists(streamId, expectedVersion);
        all.addAll(toAppend);
        events.addAll(toAppend);

        notifyListeners(streamId, toAppend, 0);

        return events.size();

    }

    @Override
    public final int appendToStream(final StreamId streamId, final int expectedVersion,
            final CommonEvent... events) {

        Contract.requireArgNotNull("events", events);

        return appendToStream(streamId, expectedVersion, Arrays.asList(events));

    }

    @Override
    public final int appendToStream(final StreamId streamId, final List<CommonEvent> toAppend) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("toAppend", toAppend);

        final List<CommonEvent> events = createIfNotExists(streamId);
        all.addAll(toAppend);
        events.addAll(toAppend);

        notifyListeners(streamId, toAppend, 0);

        return events.size();

    }

    @Override
    public final int appendToStream(final StreamId streamId, final CommonEvent... events) {

        Contract.requireArgNotNull("events", events);

        return appendToStream(streamId, Arrays.asList(events));

    }

    @Override
    public final Subscription subscribeToStream(final StreamId streamId, final int eventNumber,
            final BiConsumer<Subscription, CommonEvent> onEvent,
            final BiConsumer<Subscription, Exception> onDrop) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("onEvent", onEvent);
        Contract.requireArgNotNull("onDrop", onDrop);

        final List<CommonEvent> events = getStream(streamId);
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

    private List<CommonEvent> getStream(final StreamId streamId) {
        return getStream(streamId, false);
    }

    private List<CommonEvent> getStream(final StreamId streamId, final boolean suppressNotFound) {
        final List<CommonEvent> events = streams.get(streamId);
        if (events == null) {
            if (deletedStreams.containsKey(streamId)) {
                throw new StreamDeletedException(streamId);
            }
            if (suppressNotFound) {
                return new ArrayList<>();
            }
            throw new StreamNotFoundException(streamId);            
        }
        return events;
    }

    private List<CommonEvent> getStream(final StreamId streamId, final int expected) {
        final List<CommonEvent> events = getStream(streamId,
                (expected == ExpectedVersion.EMPTY_STREAM.getNo()));
        final int actual = events.size() - 1;
        if ((actual == -1) && (expected == ExpectedVersion.EMPTY_STREAM.getNo())) {
            return events;
        }
        if (expected != ExpectedVersion.ANY.getNo() && expected != actual) {
            throw new StreamVersionConflictException(streamId, expected, actual);
        }
        return events;
    }

    private List<CommonEvent> createIfNotExists(final StreamId streamId) {

        try {
            return getStream(streamId);
        } catch (final StreamNotFoundException ex) {
            final List<CommonEvent> events = new ArrayList<CommonEvent>();
            streams.put(streamId, events);
            return events;
        }
    }

    private List<CommonEvent> createIfNotExists(final StreamId streamId, final int expected) {

        try {
            return getStream(streamId, expected);
        } catch (final StreamNotFoundException ex) {
            final List<CommonEvent> events = new ArrayList<CommonEvent>();
            streams.put(streamId, events);
            return events;
        }
    }

    /**
     * Internal structure to store subscriptions and the listeners together.
     */
    private static final class InternalSubscription {

        private InMemorySubscription subscription;

        private BiConsumer<Subscription, CommonEvent> eventListener;

        /**
         * Constructor for find operations.
         * 
         * @param subscription
         *            The subscription.
         */
        public InternalSubscription(final InMemorySubscription subscription) {
            super();
            this.subscription = subscription;
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
