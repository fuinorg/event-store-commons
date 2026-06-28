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

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.EventStoreAsync;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.SubscribableEventStore;
import org.fuin.esc.api.SubscribableEventStoreAsync;
import org.fuin.esc.api.Subscription;
import org.fuin.esc.spi.AbstractReadableEventStore;
import org.fuin.objects4j.common.Contract;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

/**
 * Adapts an asynchronous {@link EventStoreAsync} to the synchronous {@link EventStore} interface by
 * blocking on the completable futures. Used to run an async implementation through the synchronous
 * Cucumber test harness. All event store exceptions are unchecked, so the cause of an
 * {@link ExecutionException} is simply re-thrown to surface the same exception the steps expect.
 */
public final class SyncFromAsyncEventStore extends AbstractReadableEventStore
        implements EventStore, SubscribableEventStore {

    private final EventStoreAsync delegate;

    /**
     * Constructor with the asynchronous store to delegate to.
     *
     * @param delegate Asynchronous event store all calls are forwarded to.
     */
    public SyncFromAsyncEventStore(final EventStoreAsync delegate) {
        super();
        this.delegate = Objects.requireNonNull(delegate, "delegate==null");
    }

    @Override
    public EventStore open() {
        await(delegate.open());
        return this;
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public boolean isSupportsCreateStream() {
        return delegate.isSupportsCreateStream();
    }

    @Override
    public void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        await(delegate.createStream(streamId));
    }

    @Override
    public long appendToStream(final StreamId streamId, final CommonEvent... events) {
        return await(delegate.appendToStream(streamId, events));
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events) {
        return await(delegate.appendToStream(streamId, expectedVersion, events));
    }

    @Override
    public long appendToStream(final StreamId streamId, final List<CommonEvent> events) {
        return await(delegate.appendToStream(streamId, events));
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final List<CommonEvent> events) {
        return await(delegate.appendToStream(streamId, expectedVersion, events));
    }

    @Override
    public void deleteStream(final StreamId streamId, final long expectedVersion, final boolean hardDelete) {
        await(delegate.deleteStream(streamId, expectedVersion, hardDelete));
    }

    @Override
    public void deleteStream(final StreamId streamId, final boolean hardDelete) {
        await(delegate.deleteStream(streamId, hardDelete));
    }

    @Override
    public StreamEventsSlice readEventsForward(final StreamId streamId, final long start, final int count) {
        return await(delegate.readEventsForward(streamId, start, count));
    }

    @Override
    public StreamEventsSlice readEventsBackward(final StreamId streamId, final long start, final int count) {
        return await(delegate.readEventsBackward(streamId, start, count));
    }

    @Override
    public CommonEvent readEvent(final StreamId streamId, final long eventNumber) {
        return await(delegate.readEvent(streamId, eventNumber));
    }

    @Override
    public boolean streamExists(final StreamId streamId) {
        return await(delegate.streamExists(streamId));
    }

    @Override
    public StreamState streamState(final StreamId streamId) {
        return await(delegate.streamState(streamId));
    }

    @Override
    public Subscription subscribeToStream(final StreamId streamId, final long eventNumber,
                                          final BiConsumer<Subscription, CommonEvent> onEvent,
                                          final BiConsumer<Subscription, Exception> onDrop) {
        if (!(delegate instanceof SubscribableEventStoreAsync subscribable)) {
            throw new UnsupportedOperationException(
                    "The delegate does not support subscriptions: " + delegate.getClass().getName());
        }
        return await(subscribable.subscribeToStream(streamId, eventNumber, onEvent, onDrop));
    }

    @Override
    public void unsubscribeFromStream(final Subscription subscription) {
        if (!(delegate instanceof SubscribableEventStoreAsync subscribable)) {
            throw new UnsupportedOperationException(
                    "The delegate does not support subscriptions: " + delegate.getClass().getName());
        }
        await(subscribable.unsubscribeFromStream(subscription));
    }

    /**
     * Waits for the result of a future and re-throws the cause of any failure. The (unchecked) event
     * store exceptions thrown by the asynchronous store are surfaced unchanged.
     *
     * @param future Future to wait for.
     * @param <T>    Type of the result.
     * @return Result of the future.
     */
    private static <T> T await(final CompletableFuture<T> future) {
        Contract.requireArgNotNull("future", future);
        try {
            return future.get();
        } catch (final ExecutionException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new RuntimeException(cause == null ? ex : cause);
        } catch (final InterruptedException ex) { // NOSONAR
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for the event store result", ex);
        }
    }

}
