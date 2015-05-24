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
package org.fuin.esc.esj;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import lt.emasina.esj.EventStore;
import lt.emasina.esj.Settings;
import lt.emasina.esj.message.ReadAllEventsForwardCompleted;
import lt.emasina.esj.model.Event;
import lt.emasina.esj.model.UserCredentials;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.StreamVersionConflictException;
import org.fuin.esc.api.WritableEventStore;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.MetaDataAccessor;
import org.fuin.esc.spi.MetaDataBuilder;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Contract;

/**
 * Adapter for the <a herf="https://github.com/valdasraps/esj">esj</a> event
 * store client {@link EventStore}.
 */
public final class EsjEventStore implements WritableEventStore {

    /**
     * Name used for querying the serializer/deserializer registry for the meta
     * data type.
     */
    public static final String META_TYPE = "MetaData";

    private final InetAddress host;

    private final int port;

    private final Settings settings;

    private final ExecutorService executor;

    private final String user;

    private final String password;

    private final EventConverter eventConverter;

    private final CommonEventConverter commonEventConverter;

    private final StreamEventsSliceConverter sliceConverter;

    private EventStore es;

    /**
     * Constructor with all mandatory data.
     * 
     * @param host
     *            Event store host address.
     * @param port
     *            Post of the event store.
     * @param settings
     *            Additional settings.
     * @param executor
     *            Executor for asynchronous calls.
     * @param user
     *            User for login.
     * @param password
     *            Password.
     * @param serRegistry
     *            Serializer registry.
     * @param deserRegistry
     *            Deserializer registry.
     * @param metaDataBuilder
     *            Builder used to create/add meta data.
     * @param metaDataAccessor
     *            Used to read fields from an unknown meta data type.
     */
    // CHECKSTYLE:OFF:ParameterNumber Not nice but OK here
    @SuppressWarnings("rawtypes")
    public EsjEventStore(final InetAddress host, final int port,
            final Settings settings, final ExecutorService executor,
            final String user, final String password,
            final SerializerRegistry serRegistry,
            final DeserializerRegistry deserRegistry,
            final MetaDataBuilder metaDataBuilder,
            final MetaDataAccessor metaDataAccessor) {
        // CHECKSTYLE:ON:ParameterNumber
        super();
        this.host = host;
        this.port = port;
        this.settings = settings;
        this.executor = executor;
        this.user = user;
        this.password = password;
        this.eventConverter = new EventConverter(serRegistry, metaDataBuilder);
        this.commonEventConverter = new CommonEventConverter(deserRegistry,
                metaDataAccessor);
        this.sliceConverter = new StreamEventsSliceConverter(deserRegistry,
                metaDataAccessor);
    }

    @Override
    public final void open() {
        try {
            es = new EventStore(host, port, settings, executor,
                    new UserCredentials(user, password));
        } catch (final IOException ex) {
            throw new RuntimeException("Error opening event store", ex);
        }
    }

    @Override
    public final void close() {
        try {
            es.close();
        } catch (final Exception ex) {
            throw new RuntimeException("Error closing event store", ex);
        }
    }

    @Override
    public final CommonEvent readEvent(final StreamId streamId,
            final int eventNumber) throws EventNotFoundException,
            StreamNotFoundException, StreamDeletedException {

        final ReadEventHandler handler = new ReadEventHandler(streamId,
                eventNumber);
        es.readFromStream(streamId.asString(), eventNumber, handler);
        return commonEventConverter.convert(handler.getResult());

    }

    @Override
    public final StreamEventsSlice readStreamEventsForward(
            final StreamId streamId, final int start, final int count)
            throws StreamNotFoundException, StreamDeletedException {

        final ReadAllEventsForwardHandler handler = new ReadAllEventsForwardHandler(
                streamId);
        es.readAllEventsForward(streamId.asString(), start, count, handler);
        final ReadAllEventsForwardCompleted result = handler.getResult();
        return sliceConverter.convert(result, start);

    }

    @Override
    public final StreamEventsSlice readAllEventsForward(final int start,
            final int count) {
        try {
            return readStreamEventsForward(new SimpleStreamId("$all"), start,
                    count);
        } catch (final StreamNotFoundException | StreamDeletedException ex) {
            throw new RuntimeException("$all should always exist, but did not",
                    ex);
        }
    }

    @Override
    public final boolean deleteStream(final StreamId streamId,
            final int expectedVersion) throws StreamNotFoundException,
            StreamVersionConflictException, StreamDeletedException {

        Contract.requireArgNotNull("streamId", streamId);

        final DeleteStreamHandler handler = new DeleteStreamHandler(streamId,
                expectedVersion);
        es.deleteStream(streamId.asString(), expectedVersion, handler);
        return handler.getResult();

    }

    @Override
    public final void deleteStream(final StreamId streamId)
            throws StreamNotFoundException, StreamDeletedException {
        try {
            deleteStream(streamId, EventStore.VERSION_ANY);
        } catch (final StreamVersionConflictException ex) {
            throw new RuntimeException(
                    "Delete any version was requested, but still got a version conflict",
                    ex);
        }
    }

    @Override
    public final int appendToStream(final StreamId streamId,
            final int expectedVersion, final CommonEvent... events)
            throws StreamNotFoundException, StreamVersionConflictException,
            StreamDeletedException, StreamReadOnlyException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("events", events);

        return appendToStream(streamId, expectedVersion, Arrays.asList(events));

    }

    @SuppressWarnings("rawtypes")
    @Override
    public final int appendToStream(final StreamId streamId,
            final int expectedVersion, final List<CommonEvent> commonEvents)
            throws StreamNotFoundException, StreamVersionConflictException,
            StreamDeletedException, StreamReadOnlyException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("commonEvents", commonEvents);

        final List<Event> esjEvents = eventConverter.convert(commonEvents);

        final AppendToStreamHandler handler = new AppendToStreamHandler(
                streamId, expectedVersion);
        es.appendToStream(streamId.asString(), expectedVersion, handler,
                esjEvents);
        return handler.getResult().getLastEventNumber();
    }

    @Override
    public final int appendToStream(final StreamId streamId,
            final List<CommonEvent> events) throws StreamNotFoundException,
            StreamDeletedException, StreamReadOnlyException {
        try {
            return appendToStream(streamId, EventStore.VERSION_ANY, events);
        } catch (final StreamVersionConflictException ex) {
            throw new RuntimeException(
                    "Append to any version was requested, but still got a version conflict",
                    ex);
        }
    }

    @Override
    public final int appendToStream(final StreamId streamId,
            final CommonEvent... events) throws StreamNotFoundException,
            StreamDeletedException, StreamReadOnlyException {
        return appendToStream(streamId, Arrays.asList(events));
    }

}
