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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Credentials;
import org.fuin.esc.api.EventStoreAsync;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;

/**
 * In-memory implementation for unit testing.
 */
public final class InMemoryEventStoreAsync implements EventStoreAsync {

    private InMemoryEventStoreSync delegate;

    /**
     * Default constructor.
     */
    public InMemoryEventStoreAsync() {
        super();
        delegate = new InMemoryEventStoreSync();
    }

    @Override
    public final CompletableFuture<Void> open() {
        delegate.open();
        return new CompletableFuture<Void>();
    }

    @Override
    public final void close() {
        delegate.close();
    }

    @Override
    public final CompletableFuture<CommonEvent> readEvent(
            final Optional<Credentials> credentials, final StreamId streamId,
            final int eventNumber) {

        final CompletableFuture<CommonEvent> cf = new CompletableFuture<>();
        cf.complete(delegate.readEvent(credentials, streamId, eventNumber));
        return cf;
    }

    @Override
    public final CompletableFuture<StreamEventsSlice> readEventsForward(
            final Optional<Credentials> credentials, final StreamId streamId,
            final int start, final int count) {

        final CompletableFuture<StreamEventsSlice> cf = new CompletableFuture<>();
        cf.complete(delegate.readEventsForward(credentials, streamId, start,
                count));
        return cf;

    }

    @Override
    public final CompletableFuture<StreamEventsSlice> readEventsBackward(
            final Optional<Credentials> credentials, final StreamId streamId,
            final int start, final int count) {

        final CompletableFuture<StreamEventsSlice> cf = new CompletableFuture<>();
        cf.complete(delegate.readEventsBackward(credentials, streamId, start,
                count

        ));
        return cf;
    }

    @Override
    public final CompletableFuture<Void> deleteStream(
            final Optional<Credentials> credentials, final StreamId streamId,
            final int expected) {

        final CompletableFuture<Void> cf = new CompletableFuture<>();
        cf.complete(null);
        return cf;

    }

    @Override
    public final CompletableFuture<Void> deleteStream(
            final Optional<Credentials> credentials, final StreamId streamId) {

        final CompletableFuture<Void> cf = new CompletableFuture<>();
        delegate.deleteStream(credentials, streamId);
        cf.complete(null);
        return cf;

    }

    @Override
    public final CompletableFuture<Integer> appendToStream(
            final Optional<Credentials> credentials, final StreamId streamId,
            final int expectedVersion, final List<CommonEvent> toAppend) {

        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.complete(delegate.appendToStream(credentials, streamId,
                expectedVersion, toAppend));
        return cf;

    }

    @Override
    public final CompletableFuture<Integer> appendToStream(
            final Optional<Credentials> credentials, final StreamId streamId,
            final int expectedVersion, final CommonEvent... events) {

        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.complete(delegate.appendToStream(credentials, streamId,
                expectedVersion, events));
        return cf;

    }

    @Override
    public final CompletableFuture<Integer> appendToStream(
            final Optional<Credentials> credentials, final StreamId streamId,
            final List<CommonEvent> toAppend) {

        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.complete(delegate.appendToStream(credentials, streamId, toAppend));
        return cf;

    }

    @Override
    public final CompletableFuture<Integer> appendToStream(
            final Optional<Credentials> credentials, final StreamId streamId,
            final CommonEvent... events) {

        final CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.complete(delegate.appendToStream(credentials, streamId, events));
        return cf;

    }

}
