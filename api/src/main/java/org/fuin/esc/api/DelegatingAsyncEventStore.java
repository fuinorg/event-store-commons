/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Contract;

/**
 * Asynchronous event store that uses a synchronous one internally.
 */
public final class DelegatingAsyncEventStore implements EventStoreAsync {

    private Executor executor;

    private EventStore delegate;

    /**
     * Constructor with all mandatory data.
     * 
     * @param executor
     *            Executor used to create the completable futures.
     * @param delegate
     *            Delegate to forward all method calls to.
     */
    public DelegatingAsyncEventStore(@NotNull final Executor executor, 
            @NotNull final EventStore delegate) {
        super();
        Contract.requireArgNotNull("executor", executor);
        Contract.requireArgNotNull("delegate", delegate);
        this.executor = executor;
        this.delegate = delegate;
    }

    @Override
    public final CompletableFuture<Void> open() {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                delegate.open();
            }
        }, executor);
    }

    @Override
    public final void close() {
        delegate.close();
    }

    @Override
    public final CompletableFuture<CommonEvent> readEvent(final StreamId streamId, final int eventNumber) {

        return CompletableFuture.supplyAsync(new Supplier<CommonEvent>() {
            @Override
            public CommonEvent get() {
                return delegate.readEvent(streamId, eventNumber);
            }
        }, executor);

    }

    @Override
    public final CompletableFuture<StreamEventsSlice> readEventsForward(final StreamId streamId,
            final int start, final int count) {

        return CompletableFuture.supplyAsync(new Supplier<StreamEventsSlice>() {
            @Override
            public StreamEventsSlice get() {
                return delegate.readEventsForward(streamId, start, count);
            }
        }, executor);

    }

    @Override
    public final CompletableFuture<StreamEventsSlice> readEventsBackward(final StreamId streamId,
            final int start, final int count) {

        return CompletableFuture.supplyAsync(new Supplier<StreamEventsSlice>() {
            @Override
            public StreamEventsSlice get() {
                return delegate.readEventsBackward(streamId, start, count);
            }
        }, executor);

    }

    @Override
    public final CompletableFuture<Void> deleteStream(final StreamId streamId, final int expected,
            final boolean hardDelete) {

        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                delegate.deleteStream(streamId, expected, hardDelete);
            }
        }, executor);

    }

    @Override
    public final CompletableFuture<Void> deleteStream(final StreamId streamId, final boolean hardDelete) {

        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                delegate.deleteStream(streamId, hardDelete);
            }
        }, executor);

    }

    @Override
    public final CompletableFuture<Integer> appendToStream(final StreamId streamId,
            final int expectedVersion, final List<CommonEvent> toAppend) {

        return CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return delegate.appendToStream(streamId, expectedVersion, toAppend);
            }
        }, executor);

    }

    @Override
    public final CompletableFuture<Integer> appendToStream(final StreamId streamId,
            final int expectedVersion, final CommonEvent... events) {

        return CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return delegate.appendToStream(streamId, expectedVersion, events);
            }
        }, executor);

    }

    @Override
    public final CompletableFuture<Integer> appendToStream(final StreamId streamId,
            final List<CommonEvent> toAppend) {

        return CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return delegate.appendToStream(streamId, toAppend);
            }
        }, executor);

    }

    @Override
    public final CompletableFuture<Integer> appendToStream(final StreamId streamId,
            final CommonEvent... events) {

        return CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return delegate.appendToStream(streamId, events);
            }
        }, executor);

    }

    @Override
    public final CompletableFuture<Boolean> streamExists(final StreamId streamId) {
        return CompletableFuture.supplyAsync(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return delegate.streamExists(streamId);
            }
        }, executor);
    }

    @Override
    public final CompletableFuture<StreamState> streamState(final StreamId streamId) {
        return CompletableFuture.supplyAsync(new Supplier<StreamState>() {
            @Override
            public StreamState get() {
                return delegate.streamState(streamId);
            }
        }, executor);
    }

}
