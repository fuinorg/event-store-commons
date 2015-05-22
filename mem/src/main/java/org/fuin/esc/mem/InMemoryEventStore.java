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

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ProjectionNotWritableException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamVersionConflictException;
import org.fuin.esc.api.WritableEventStore;
import org.fuin.objects4j.common.Contract;

/**
 * In-memory implementation for unit testing.
 */
public class InMemoryEventStore implements WritableEventStore {

    private List<CommonEvent> all;

    private Map<StreamId, List<CommonEvent>> streams;

    private Map<StreamId, List<CommonEvent>> deletedStreams;

    public InMemoryEventStore() {
        super();
        all = new ArrayList<CommonEvent>();
        streams = new HashMap<StreamId, List<CommonEvent>>();
        deletedStreams = new HashMap<StreamId, List<CommonEvent>>();
    }

    @Override
    public void open() {
        // Do nothing
    }

    @Override
    public void close() {
        // Do nothing
    }

    @Override
    public CommonEvent readEvent(final StreamId streamId, final int eventNumber)
            throws EventNotFoundException, StreamNotFoundException,
            StreamDeletedException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);

        final List<CommonEvent> events = getStream(streamId);
        if (events.size() - 1 < eventNumber) {
            throw new EventNotFoundException(streamId, eventNumber);
        }
        return events.get(eventNumber);
    }

    @Override
    public StreamEventsSlice readStreamEventsForward(StreamId streamId,
            int start, int count) throws StreamNotFoundException,
            StreamDeletedException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);

        final List<CommonEvent> events = getStream(streamId);

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
    public StreamEventsSlice readAllEventsForward(int start, int count) {

        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);

        final List<CommonEvent> result = new ArrayList<CommonEvent>();
        for (int i = start; (i < (start + count)) && (i < all.size()); i++) {
            result.add(all.get(i));
        }
        final int fromEventNumber = start;
        final int nextEventNumber = (start + result.size());
        final boolean endOfStream = (result.size() < count);
        return new StreamEventsSlice(fromEventNumber, result, nextEventNumber,
                endOfStream);

    }

    @Override
    public void deleteStream(StreamId streamId, int expected)
            throws StreamNotFoundException, StreamVersionConflictException,
            StreamDeletedException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expected", expected, 0);

        final List<CommonEvent> events = getStream(streamId, expected);
        deletedStreams.put(streamId, events);
        streams.remove(streamId);

    }

    @Override
    public void deleteStream(StreamId streamId) throws StreamNotFoundException,
            StreamDeletedException {

        Contract.requireArgNotNull("streamId", streamId);

        final List<CommonEvent> events = getStream(streamId);
        deletedStreams.put(streamId, events);
        streams.remove(streamId);

    }

    @Override
    public int appendToStream(final StreamId streamId,
            final int expectedVersion, final List<CommonEvent> toAppend)
            throws StreamNotFoundException, StreamVersionConflictException,
            StreamDeletedException, ProjectionNotWritableException {

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
    public int appendToStream(StreamId streamId, int expectedVersion,
            CommonEvent... events) throws StreamNotFoundException,
            StreamVersionConflictException, StreamDeletedException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, 0);
        Contract.requireArgNotNull("events", events);

        return appendToStream(streamId, expectedVersion, Arrays.asList(events));

    }

    @Override
    public int appendToStream(final StreamId streamId,
            final List<CommonEvent> toAppend) throws StreamNotFoundException,
            StreamDeletedException, ProjectionNotWritableException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("toAppend", toAppend);

        final List<CommonEvent> events = createIfNotExists(streamId);
        all.addAll(toAppend);
        events.addAll(toAppend);
        return events.size() - 1;

    }

    @Override
    public int appendToStream(StreamId streamId, CommonEvent... events)
            throws StreamNotFoundException, StreamDeletedException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("events", events);

        return appendToStream(streamId, Arrays.asList(events));

    }

    private List<CommonEvent> getStream(final StreamId streamId)
            throws StreamDeletedException, StreamNotFoundException {
        final List<CommonEvent> events = streams.get(streamId);
        if (events == null) {
            if (deletedStreams.containsKey(streamId)) {
                throw new StreamDeletedException(streamId);
            }
            throw new StreamNotFoundException(streamId);
        }
        return events;
    }

    private List<CommonEvent> getStream(final StreamId streamId, int expected)
            throws StreamDeletedException, StreamNotFoundException,
            StreamVersionConflictException {
        final List<CommonEvent> events = getStream(streamId);
        final int actual = events.size() - 1;
        if (expected != actual) {
            throw new StreamVersionConflictException(streamId, expected, actual);
        }
        return events;
    }

    private List<CommonEvent> createIfNotExists(final StreamId streamId)
            throws StreamDeletedException {

        try {
            return getStream(streamId);
        } catch (final StreamNotFoundException ex) {
            final List<CommonEvent> events = new ArrayList<CommonEvent>();
            streams.put(streamId, events);
            return events;
        }
    }

    private List<CommonEvent> createIfNotExists(final StreamId streamId,
            int expected) throws StreamDeletedException,
            StreamVersionConflictException {

        try {
            return getStream(streamId, expected);
        } catch (final StreamNotFoundException ex) {
            final List<CommonEvent> events = new ArrayList<CommonEvent>();
            streams.put(streamId, events);
            return events;
        }
    }

}
