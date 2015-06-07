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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Credentials;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamVersionConflictException;
import org.fuin.objects4j.common.Contract;

/**
 * In-memory implementation for unit testing.
 */
public final class InMemoryEventStoreSync implements EventStoreSync {

    private List<CommonEvent> all;

    private Map<StreamId, List<CommonEvent>> streams;

    private Map<StreamId, List<CommonEvent>> deletedStreams;

    /**
     * Default constructor.
     */
    public InMemoryEventStoreSync() {
        super();
        all = new ArrayList<CommonEvent>();
        streams = new HashMap<StreamId, List<CommonEvent>>();
        deletedStreams = new HashMap<StreamId, List<CommonEvent>>();
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
    public final CommonEvent readEvent(final Optional<Credentials> credentials,
            final StreamId streamId, final int eventNumber) {

        Contract.requireArgNotNull("credentials", credentials);
        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);

        final List<CommonEvent> events = getStream(streamId);
        if (events.size() - 1 < eventNumber) {
            throw new EventNotFoundException(streamId, eventNumber);
        }

        return events.get(eventNumber);
    }

    @Override
    public final StreamEventsSlice readEventsForward(
            final Optional<Credentials> credentials, final StreamId streamId,
            final int start, final int count) {

        Contract.requireArgNotNull("credentials", credentials);
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

        return new StreamEventsSlice(fromEventNumber, result, nextEventNumber,
                endOfStream);

    }

    @Override
    public final StreamEventsSlice readEventsBackward(
            final Optional<Credentials> credentials, final StreamId streamId,
            final int start, final int count) {

        Contract.requireArgNotNull("credentials", credentials);
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
        for (int i = start; (i > (start - count)) && (i >= 0); i--) {
            result.add(events.get(i));
        }

        final int fromEventNumber = start;
        final int nextEventNumber = start - result.size();
        final boolean endOfStream = (start - count) < 0;

        return new StreamEventsSlice(fromEventNumber, result, nextEventNumber,
                endOfStream);
    }

    @Override
    public final void deleteStream(final Optional<Credentials> credentials,
            final StreamId streamId, final int expected) {

        Contract.requireArgNotNull("credentials", credentials);
        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expected", expected, 0);
        if (streamId == StreamId.ALL) {
            throw new IllegalArgumentException(
                    "It's not possible to delete the 'all' stream");
        }

        final List<CommonEvent> events = getStream(streamId, expected);
        deletedStreams.put(streamId, events);
        streams.remove(streamId);

    }

    @Override
    public final void deleteStream(final Optional<Credentials> credentials,
            final StreamId streamId) {

        Contract.requireArgNotNull("credentials", credentials);
        Contract.requireArgNotNull("streamId", streamId);

        final List<CommonEvent> events = getStream(streamId);
        deletedStreams.put(streamId, events);
        streams.remove(streamId);

    }

    @Override
    public final int appendToStream(final Optional<Credentials> credentials,
            final StreamId streamId, final int expectedVersion,
            final List<CommonEvent> toAppend) {

        Contract.requireArgNotNull("credentials", credentials);
        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, 0);
        Contract.requireArgNotNull("toAppend", toAppend);

        final List<CommonEvent> events = createIfNotExists(streamId,
                expectedVersion);
        all.addAll(toAppend);
        events.addAll(toAppend);

        return events.size() - 1;

    }

    @Override
    public final int appendToStream(final Optional<Credentials> credentials,
            final StreamId streamId, final int expectedVersion,
            final CommonEvent... events) {

        Contract.requireArgNotNull("events", events);

        return appendToStream(credentials, streamId, expectedVersion,
                Arrays.asList(events));

    }

    @Override
    public final int appendToStream(final Optional<Credentials> credentials,
            final StreamId streamId, final List<CommonEvent> toAppend) {

        Contract.requireArgNotNull("credentials", credentials);
        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("toAppend", toAppend);

        final List<CommonEvent> events = createIfNotExists(streamId);
        all.addAll(toAppend);
        events.addAll(toAppend);

        return events.size() - 1;

    }

    @Override
    public final int appendToStream(final Optional<Credentials> credentials,
            final StreamId streamId, final CommonEvent... events) {

        Contract.requireArgNotNull("events", events);

        return appendToStream(credentials, streamId, Arrays.asList(events));

    }

    private List<CommonEvent> getStream(final StreamId streamId) {
        final List<CommonEvent> events = streams.get(streamId);
        if (events == null) {
            if (deletedStreams.containsKey(streamId)) {
                throw new StreamDeletedException(streamId);
            }
            throw new StreamNotFoundException(streamId);
        }
        return events;
    }

    private List<CommonEvent> getStream(final StreamId streamId,
            final int expected) {
        final List<CommonEvent> events = getStream(streamId);
        final int actual = events.size() - 1;
        if (expected != actual) {
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

    private List<CommonEvent> createIfNotExists(final StreamId streamId,
            final int expected) {

        try {
            return getStream(streamId, expected);
        } catch (final StreamNotFoundException ex) {
            final List<CommonEvent> events = new ArrayList<CommonEvent>();
            streams.put(streamId, events);
            return events;
        }
    }

}
