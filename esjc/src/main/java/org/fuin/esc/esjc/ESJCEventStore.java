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
package org.fuin.esc.esjc;

import static org.fuin.esc.api.ExpectedVersion.ANY;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Contract;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.EventReadResult;
import com.github.msemys.esjc.EventReadStatus;
import com.github.msemys.esjc.ResolvedEvent;
import com.github.msemys.esjc.SliceReadStatus;
import com.github.msemys.esjc.StreamMetadataResult;
import com.github.msemys.esjc.WriteResult;

/**
 * Implementation that connects to the event store
 * (http://www.geteventstore.com) using the esjc
 * (https://github.com/msemys/esjc) API.
 */
public final class ESJCEventStore implements EventStore {

    private final com.github.msemys.esjc.EventStore es;

    private final CommonEvent2EventDataConverter ce2edConv;

    private final RecordedEvent2CommonEventConverter ed2ceConv;

    private boolean open;

    /**
     * Constructor with event store to use.
     * 
     * @param es
     *            Delegate.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     * @param targetContentType
     *            Target content type (Allows only 'application/xml' or
     *            'application/json' with 'utf-8' encoding).
     */
    public ESJCEventStore(@NotNull final com.github.msemys.esjc.EventStore es,
            @NotNull final SerializerRegistry serRegistry, @NotNull final DeserializerRegistry desRegistry,
            @NotNull final EnhancedMimeType targetContentType) {
        super();
        Contract.requireArgNotNull("es", es);
        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("desRegistry", desRegistry);
        Contract.requireArgNotNull("targetContentType", targetContentType);
        this.es = es;
        this.ce2edConv = new CommonEvent2EventDataConverter(serRegistry, targetContentType);
        this.ed2ceConv = new RecordedEvent2CommonEventConverter(desRegistry);
        this.open = false;
    }

    @Override
    public final void open() {
        if (open) {
            // Ignore
            return;
        }
        es.connect();
        this.open = true;
    }

    @Override
    public final void close() {
        if (!open) {
            // Ignore
            return;
        }
        es.disconnect();
        this.open = false;
    }

    @Override
    public final boolean isSupportsCreateStream() {
        return false;
    }

    @Override
    public final void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Do nothing as the operation is not supported
    }

    @Override
    public final int appendToStream(final StreamId streamId, final CommonEvent... events)
            throws StreamNotFoundException, StreamDeletedException, StreamReadOnlyException {
        return appendToStream(streamId, -2, EscSpiUtils.asList(events));
    }

    @Override
    public final int appendToStream(final StreamId streamId, final int expectedVersion,
            final CommonEvent... events) throws StreamNotFoundException, StreamDeletedException,
            WrongExpectedVersionException, StreamReadOnlyException {
        return appendToStream(streamId, expectedVersion, EscSpiUtils.asList(events));
    }

    @Override
    public int appendToStream(final StreamId streamId, final List<CommonEvent> events)
            throws StreamNotFoundException, StreamDeletedException, StreamReadOnlyException {
        return appendToStream(streamId, -2, events);
    }

    @Override
    public final int appendToStream(final StreamId streamId, final int expectedVersion,
            final List<CommonEvent> commonEvents)
            throws StreamDeletedException, WrongExpectedVersionException, StreamReadOnlyException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        Contract.requireArgNotNull("commonEvents", commonEvents);
        ensureOpen();

        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }

        try {
            final Iterable<EventData> eventDataIt = asEventData(commonEvents);
            final WriteResult result = es.appendToStream(streamId.asString(),
                    com.github.msemys.esjc.ExpectedVersion.of(expectedVersion), eventDataIt).get();
            return result.nextExpectedVersion;
        } catch (final ExecutionException ex) {
            if (ex.getCause() instanceof com.github.msemys.esjc.operation.WrongExpectedVersionException) {
                // TODO Add actual version instead of NULL if ES returns this
                // some day
                throw new WrongExpectedVersionException(streamId, expectedVersion, null);
            }
            if (ex.getCause() instanceof com.github.msemys.esjc.operation.StreamDeletedException) {
                throw new StreamDeletedException(streamId);
            }
            throw new RuntimeException("Error executing append", ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException("Error waiting for append result", ex);
        }

    }

    @Override
    public final void deleteStream(final StreamId streamId, final int expectedVersion,
            final boolean hardDelete) throws StreamDeletedException, WrongExpectedVersionException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        ensureOpen();

        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }

        try {
            es.deleteStream(streamId.asString(), com.github.msemys.esjc.ExpectedVersion.of(expectedVersion),
                    hardDelete).get();
        } catch (final ExecutionException ex) {
            if (ex.getCause() instanceof com.github.msemys.esjc.operation.WrongExpectedVersionException) {
                // TODO Add actual version instead of NULL if ES returns this
                // some day
                throw new WrongExpectedVersionException(streamId, expectedVersion, null);
            }
            if (ex.getCause() instanceof com.github.msemys.esjc.operation.StreamDeletedException) {
                throw new StreamDeletedException(streamId);
            }
            throw new RuntimeException("Error executing delete", ex);
        } catch (final InterruptedException ex) {
            throw new RuntimeException("Error waiting for delete result", ex);
        }

    }

    @Override
    public final void deleteStream(final StreamId streamId, final boolean hardDelete)
            throws StreamNotFoundException, StreamDeletedException {

        deleteStream(streamId, ANY.getNo(), hardDelete);

    }

    @Override
    public final StreamEventsSlice readEventsForward(final StreamId streamId, final int start,
            final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        try {
            final com.github.msemys.esjc.StreamEventsSlice slice = es
                    .readStreamEventsForward(streamId.asString(), start, count, true).get();
            if (SliceReadStatus.StreamDeleted == slice.status) {
                throw new StreamDeletedException(streamId);
            }
            if (SliceReadStatus.StreamNotFound == slice.status) {
                throw new StreamNotFoundException(streamId);
            }
            final List<CommonEvent> events = asCommonEvents(slice.events);
            final boolean endOfStream = count > events.size();
            return new StreamEventsSlice(slice.fromEventNumber, events, slice.nextEventNumber, endOfStream);
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Error waiting for read forward result", ex);
        }

    }

    @Override
    public final StreamEventsSlice readEventsBackward(final StreamId streamId, final int start,
            final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        try {
            final com.github.msemys.esjc.StreamEventsSlice slice = es
                    .readStreamEventsBackward(streamId.asString(), start, count, true).get();
            if (SliceReadStatus.StreamDeleted == slice.status) {
                throw new StreamDeletedException(streamId);
            }
            if (SliceReadStatus.StreamNotFound == slice.status) {
                throw new StreamNotFoundException(streamId);
            }
            final List<CommonEvent> events = asCommonEvents(slice.events);
            int nextEventNumber = slice.nextEventNumber;
            final boolean endOfStream = (start - count < 0);
            if (endOfStream) {
                nextEventNumber = 0;
            }
            return new StreamEventsSlice(slice.fromEventNumber, events, nextEventNumber, endOfStream);
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Error waiting for read forward result", ex);
        }

    }

    @Override
    public final CommonEvent readEvent(final StreamId streamId, final int eventNumber) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);
        ensureOpen();

        try {
            final EventReadResult eventReadResult = es.readEvent(streamId.asString(), eventNumber, true)
                    .get();
            if (eventReadResult.status == EventReadStatus.NoStream) {
                throw new StreamNotFoundException(streamId);
            }
            if (eventReadResult.status == EventReadStatus.NotFound) {
                throw new EventNotFoundException(streamId, eventNumber);
            }
            if (eventReadResult.status == EventReadStatus.StreamDeleted) {
                throw new StreamDeletedException(streamId);
            }
            return asCommonEvent(eventReadResult.event);
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Error waiting for read forward result", ex);
        }

    }

    @Override
    public final boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        try {
            final com.github.msemys.esjc.StreamEventsSlice slice = es
                    .readStreamEventsForward(streamId.asString(), 0, 1, false).get();
            if (SliceReadStatus.StreamDeleted == slice.status) {
                return false;
            }
            if (SliceReadStatus.StreamNotFound == slice.status) {
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Error waiting for read forward result", ex);
        }

    }

    @Override
    public final StreamState streamState(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        try {

            final com.github.msemys.esjc.StreamEventsSlice slice = es
                    .readStreamEventsForward(streamId.asString(), 0, 1, false).get();
            if (SliceReadStatus.StreamDeleted == slice.status) {
                final StreamMetadataResult result = es.getStreamMetadata(streamId.asString()).get();
                if (result.isStreamDeleted) {
                    return StreamState.HARD_DELETED;
                }
                return StreamState.SOFT_DELETED;
            }
            if (SliceReadStatus.StreamNotFound == slice.status) {
                throw new StreamNotFoundException(streamId);
            }
            return StreamState.ACTIVE;

        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Error waiting for read forward result", ex);
        }

    }

    private List<EventData> asEventData(final List<CommonEvent> commonEvents) {
        final List<EventData> list = new ArrayList<>(commonEvents.size());
        for (final CommonEvent commonEvent : commonEvents) {
            list.add(ce2edConv.convert(commonEvent));
        }
        return list;
    }

    private List<CommonEvent> asCommonEvents(final List<ResolvedEvent> resolvedEvents) {
        final List<CommonEvent> list = new ArrayList<>(resolvedEvents.size());
        for (final ResolvedEvent resolvedEvent : resolvedEvents) {
            list.add(asCommonEvent(resolvedEvent));
        }
        return list;
    }

    private CommonEvent asCommonEvent(final ResolvedEvent resolvedEvent) {
        return ed2ceConv.convert(resolvedEvent.event);
    }

    private void ensureOpen() {
        if (!open) {
            open();
        }
    }

}
