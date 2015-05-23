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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import lt.emasina.esj.EventStore;
import lt.emasina.esj.ResponseReceiver;
import lt.emasina.esj.Settings;
import lt.emasina.esj.message.WriteEventsCompleted;
import lt.emasina.esj.model.Event;
import lt.emasina.esj.model.Message;
import lt.emasina.esj.model.UserCredentials;
import lt.emasina.esj.model.converter.ByteArrayToByteStringConverter;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ProjectionNotWritableException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamVersionConflictException;
import org.fuin.esc.api.WritableEventStore;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.MetaDataBuilder;
import org.fuin.esc.spi.Serializer;
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

    private final SerializerRegistry serRegistry;

    private final DeserializerRegistry deserRegistry;

    private final MetaDataBuilder metaDataBuilder;

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
     */
    public EsjEventStore(final InetAddress host, final int port,
            final Settings settings, final ExecutorService executor,
            final String user, final String password,
            final SerializerRegistry serRegistry,
            final DeserializerRegistry deserRegistry,
            final MetaDataBuilder metaDataBuilder) {
        super();
        this.host = host;
        this.port = port;
        this.settings = settings;
        this.executor = executor;
        this.user = user;
        this.password = password;
        this.serRegistry = serRegistry;
        this.deserRegistry = deserRegistry;
        this.metaDataBuilder = metaDataBuilder;
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
        // TODO Implement!
        return null;
    }

    @Override
    public final StreamEventsSlice readStreamEventsForward(
            final StreamId streamId, final int start, final int count)
            throws StreamNotFoundException, StreamDeletedException {
        // TODO Implement!
        return null;
    }

    @Override
    public final StreamEventsSlice readAllEventsForward(final int start,
            final int count) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final void deleteStream(final StreamId streamId,
            final int expectedVersion) throws StreamNotFoundException,
            StreamVersionConflictException, StreamDeletedException {
        // TODO Auto-generated method stub
    }

    @Override
    public final void deleteStream(final StreamId streamId)
            throws StreamNotFoundException, StreamDeletedException {
        // TODO Auto-generated method stub

    }

    @Override
    public final int appendToStream(final StreamId streamId,
            final int expectedVersion, final CommonEvent... events)
            throws StreamNotFoundException, StreamVersionConflictException,
            StreamDeletedException, ProjectionNotWritableException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("events", events);

        return appendToStream(streamId, expectedVersion, Arrays.asList(events));

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public final int appendToStream(final StreamId streamId,
            final int expectedVersion, final List<CommonEvent> commonEvents)
            throws StreamNotFoundException, StreamVersionConflictException,
            StreamDeletedException, ProjectionNotWritableException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("commonEvents", commonEvents);

        final List<Event> esjEvents = new ArrayList<Event>();
        for (final CommonEvent commonEvent : commonEvents) {

            final UUID id = UUID.fromString(commonEvent.getId());
            final String type = commonEvent.getType();

            final Object data = commonEvent.getData();
            final Serializer dataSer = serRegistry.getSerializer(type);
            final byte[] dataBytes = dataSer.marshal(data);

            final Serializer metaSer = serRegistry.getSerializer(META_TYPE);
            metaDataBuilder.init(commonEvent.getMeta());
            metaDataBuilder.add("content-type", metaSer.getMimeType()
                    .toString());
            final Object meta = metaDataBuilder.build();
            final byte[] metaBytes = metaSer.marshal(meta);

            // Prepare converter
            final ByteArrayToByteStringConverter dataConv = new ByteArrayToByteStringConverter(
                    dataSer.getMimeType().isJson());
            final ByteArrayToByteStringConverter metaConv = new ByteArrayToByteStringConverter(
                    metaSer.getMimeType().isJson());

            esjEvents.add(new Event(id, type, dataBytes, dataConv, metaBytes, metaConv));

        }

        es.appendToStream(streamId.asString(), expectedVersion,
                new ResponseReceiver() {
                    @Override
                    public void onResponseReturn(final Message msg) {
                        System.out.println(msg.getMessage());
                    }

                    @Override
                    public void onErrorReturn(final Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }, esjEvents);

        return 0;
    }

    @Override
    public final int appendToStream(final StreamId streamId,
            final List<CommonEvent> events) throws StreamNotFoundException,
            StreamDeletedException, ProjectionNotWritableException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public final int appendToStream(final StreamId streamId,
            final CommonEvent... events) throws StreamNotFoundException,
            StreamDeletedException {
        // TODO Auto-generated method stub
        return 0;
    }

}
