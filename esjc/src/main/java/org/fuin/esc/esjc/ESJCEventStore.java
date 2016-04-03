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

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
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
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.objects4j.common.Contract;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.ResolvedEvent;
import com.github.msemys.esjc.WriteResult;

/**
 * Implementation that connects to the event store (http://www.geteventstore.com) using the esjc
 * (https://github.com/msemys/esjc) API.
 */
public final class ESJCEventStore implements EventStore {

    private final com.github.msemys.esjc.EventStore es;

    /**
     * Constructor with event store to use.
     * 
     * @param es
     *            Delegate.
     */
    public ESJCEventStore(@NotNull final com.github.msemys.esjc.EventStore es) {
        super();
        Contract.requireArgNotNull("es", es);
        this.es = es;
    }

    @Override
    public final void open() {
        es.connect();
    }

    @Override
    public final void close() {
        es.disconnect();
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
            final List<CommonEvent> commonEvents) throws StreamDeletedException,
            WrongExpectedVersionException, StreamReadOnlyException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        Contract.requireArgNotNull("commonEvents", commonEvents);

        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }

        try {
            Iterable<EventData> e = null;
            final WriteResult result = es.appendToStream(streamId.asString(),
                    com.github.msemys.esjc.ExpectedVersion.of(expectedVersion), e).get();
            return result.nextExpectedVersion;
        } catch (final com.github.msemys.esjc.operation.WrongExpectedVersionException ex) {
            // TODO Add actual version instead of NULL if ES returns this some day
            throw new WrongExpectedVersionException(streamId, expectedVersion, null);
        } catch (final com.github.msemys.esjc.operation.StreamDeletedException ex) {
            throw new StreamDeletedException(streamId);
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Error waiting for append result", ex);
        }

    }

    @Override
    public final void deleteStream(final StreamId streamId, final int expectedVersion,
            final boolean hardDelete) throws StreamDeletedException, WrongExpectedVersionException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());

        try {
            es.deleteStream(streamId.asString(), com.github.msemys.esjc.ExpectedVersion.of(expectedVersion),
                    hardDelete).get();
        } catch (final com.github.msemys.esjc.operation.WrongExpectedVersionException ex) {
            // TODO Add actual version instead of NULL if ES returns this some day
            throw new WrongExpectedVersionException(streamId, expectedVersion, null);
        } catch (final com.github.msemys.esjc.operation.StreamDeletedException ex) {
            throw new StreamDeletedException(streamId);
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Error waiting for delete result", ex);
        }

    }

    @Override
    public final void deleteStream(final StreamId streamId, final boolean hardDelete)
            throws StreamNotFoundException, StreamDeletedException {

        deleteStream(streamId, ANY.getNo(), hardDelete);

    }

    @Override
    public final StreamEventsSlice readEventsForward(final StreamId streamId, final int start, final int count) {

        try {
            final com.github.msemys.esjc.StreamEventsSlice slice = es.readStreamEventsForward(
                    streamId.asString(), start, count, true).get();

            final List<ResolvedEvent> events = slice.events;
            for (final ResolvedEvent event : events) {
                
                
            }

            // return new StreamEventsSlice(slice.fromEventNumber, commonEvents, slice.nextEventNumber,  slice.isEndOfStream);

            return null;
            
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException("Error waiting for read forward result", ex);
        }
    }

    @Override
    public final StreamEventsSlice readEventsBackward(final StreamId streamId, final int start,
            final int count) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final CommonEvent readEvent(final StreamId streamId, final int eventNumber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final boolean streamExists(final StreamId streamId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public final StreamState streamState(final StreamId streamId) {
        // TODO Auto-generated method stub
        return null;
    }

}
