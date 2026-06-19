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
package org.fuin.esc.crypto;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.spi.AbstractReadableEventStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal in-memory {@link EventStore} for tests that stores {@link CommonEvent} objects without serialization.
 */
public final class TestEventStore extends AbstractReadableEventStore implements EventStore {

    private final Map<String, List<CommonEvent>> streams = new HashMap<>();

    public List<CommonEvent> events(final StreamId streamId) {
        return streams.getOrDefault(streamId.asString(), new ArrayList<>());
    }

    private List<CommonEvent> stream(final StreamId streamId) {
        return streams.computeIfAbsent(streamId.asString(), k -> new ArrayList<>());
    }

    @Override
    public EventStore open() {
        return this;
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public boolean isSupportsCreateStream() {
        return true;
    }

    @Override
    public void createStream(final StreamId streamId) {
        stream(streamId);
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events) {
        return appendToStream(streamId, List.of(events));
    }

    @Override
    public long appendToStream(final StreamId streamId, final CommonEvent... events) {
        return appendToStream(streamId, List.of(events));
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final List<CommonEvent> events) {
        return appendToStream(streamId, events);
    }

    @Override
    public long appendToStream(final StreamId streamId, final List<CommonEvent> events) {
        final List<CommonEvent> list = stream(streamId);
        list.addAll(events);
        return list.size();
    }

    @Override
    public void deleteStream(final StreamId streamId, final long expectedVersion, final boolean hardDelete) {
        streams.remove(streamId.asString());
    }

    @Override
    public void deleteStream(final StreamId streamId, final boolean hardDelete) {
        streams.remove(streamId.asString());
    }

    @Override
    public StreamEventsSlice readEventsForward(final StreamId streamId, final long start, final int count) {
        final List<CommonEvent> all = events(streamId);
        final int from = (int) start;
        final List<CommonEvent> result = new ArrayList<>();
        for (int i = from; i < all.size() && result.size() < count; i++) {
            result.add(all.get(i));
        }
        final long next = from + result.size();
        return new StreamEventsSlice(start, result, next, next >= all.size());
    }

    @Override
    public StreamEventsSlice readEventsBackward(final StreamId streamId, final long start, final int count) {
        final List<CommonEvent> all = events(streamId);
        final int from = (int) start;
        final List<CommonEvent> result = new ArrayList<>();
        for (int i = from; i >= 0 && result.size() < count; i--) {
            result.add(all.get(i));
        }
        final long next = from - result.size();
        return new StreamEventsSlice(start, result, next, next < 0);
    }

    @Override
    public CommonEvent readEvent(final StreamId streamId, final long eventNumber) {
        return events(streamId).get((int) eventNumber);
    }

    @Override
    public boolean streamExists(final StreamId streamId) {
        return streams.containsKey(streamId.asString());
    }

    @Override
    public StreamState streamState(final StreamId streamId) {
        return StreamState.ACTIVE;
    }

}
