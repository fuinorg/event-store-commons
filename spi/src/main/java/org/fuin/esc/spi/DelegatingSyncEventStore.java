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
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.EventStoreAsync;
import org.fuin.esc.api.EventStoreCapabilities;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamState;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.utils4j.TestOmitted;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Maps the synchronous {@link EventStore} API onto an asynchronous
 * {@link EventStoreAsync} by blocking on the returned futures. This is the synchronous counterpart to
 * {@link org.fuin.esc.api.DelegatingAsyncEventStore}. As the event store exceptions are all unchecked,
 * the cause of a failed future is simply re-thrown so callers observe the same exception the asynchronous
 * store produced. Thread-safety is inherited from the wrapped delegate.
 */
@ThreadSafe
@TestOmitted("Tested via InMemoryEventStore (esc-mem) and the cucumber 'test' project")
public class DelegatingSyncEventStore extends AbstractReadableEventStore implements EventStore {

    private final EventStoreAsync delegate;

    /**
     * Constructor with the asynchronous store to delegate to.
     *
     * @param delegate Asynchronous event store all calls are forwarded to.
     */
    public DelegatingSyncEventStore(final EventStoreAsync delegate) {
        super();
        Contract.requireArgNotNull("delegate", delegate);
        this.delegate = delegate;
    }

    @Override
    public DelegatingSyncEventStore open() {
        await(delegate.open());
        return this;
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public EventStoreCapabilities capabilities() {
        return delegate.capabilities();
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
    public boolean streamExists(final StreamId streamId) {
        return await(delegate.streamExists(streamId));
    }

    @Override
    public CommonEvent readEvent(final StreamId streamId, final long eventNumber) {
        return await(delegate.readEvent(streamId, eventNumber));
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
    public void deleteStream(final StreamId streamId, final long expected, final boolean hardDelete) {
        await(delegate.deleteStream(streamId, expected, hardDelete));
    }

    @Override
    public void deleteStream(final StreamId streamId, final boolean hardDelete) {
        await(delegate.deleteStream(streamId, hardDelete));
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final List<CommonEvent> toAppend) {
        return await(delegate.appendToStream(streamId, expectedVersion, toAppend));
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events) {
        return await(delegate.appendToStream(streamId, expectedVersion, events));
    }

    @Override
    public long appendToStream(final StreamId streamId, final List<CommonEvent> toAppend) {
        return await(delegate.appendToStream(streamId, toAppend));
    }

    @Override
    public long appendToStream(final StreamId streamId, final CommonEvent... events) {
        return await(delegate.appendToStream(streamId, events));
    }

    @Override
    public StreamState streamState(final StreamId streamId) {
        return await(delegate.streamState(streamId));
    }

    /**
     * Waits for the result of a future and re-throws the cause of any failure. The (unchecked) event store exceptions thrown by the
     * asynchronous store are surfaced unchanged.
     *
     * @param future Future to wait for.
     * @param <T>    Type of the result.
     * @return Result of the future.
     */
    private static <T> T await(final CompletableFuture<T> future) {
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
