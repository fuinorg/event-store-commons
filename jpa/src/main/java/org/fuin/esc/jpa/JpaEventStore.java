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
package org.fuin.esc.jpa;

import static org.fuin.esc.api.ExpectedVersion.ANY;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerializedData;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA Implementation of the event store.
 */
public final class JpaEventStore extends AbstractJpaEventStore implements EventStore {

    private static final Logger LOG = LoggerFactory.getLogger(JpaEventStore.class);

    private JpaIdStreamFactory streamFactory;

    /**
     * Constructor with all mandatory data.
     * 
     * @param em
     *            Entity manager.
     * @param streamFactory
     *            Stream factory.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     */
    public JpaEventStore(@NotNull final EntityManager em, @NotNull final JpaIdStreamFactory streamFactory,
            @NotNull final SerializerRegistry serRegistry, @NotNull final DeserializerRegistry desRegistry) {
        super(em, serRegistry, desRegistry);
        Contract.requireArgNotNull("streamFactory", streamFactory);
        this.streamFactory = streamFactory;
    }

    @Override
    public final void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Do nothing as the operation is not supported
    }

    @Override
    public final int appendToStream(final StreamId streamId, final CommonEvent... events) {
        return appendToStream(streamId, ANY.getNo(), EscSpiUtils.asList(events));
    }

    @Override
    public final int appendToStream(final StreamId streamId, final int expectedVersion,
            final CommonEvent... events) {
        return appendToStream(streamId, expectedVersion, EscSpiUtils.asList(events));
    }

    @Override
    public final int appendToStream(final StreamId streamId, final List<CommonEvent> events) {
        return appendToStream(streamId, ANY.getNo(), events);
    }

    @Override
    public final int appendToStream(final StreamId streamId, final int expectedVersion,
            final List<CommonEvent> toAppend) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        Contract.requireArgNotNull("toAppend", toAppend);

        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }
        
        JpaStream stream = findAndLockJpaStream(streamId);
        if (stream == null) {
            LOG.debug("Stream '{}' not found, creating it", streamId);
            stream = streamFactory.createStream(streamId);
            getEm().persist(stream);
        } else {
            LOG.debug("Stream '{}' found, reading it", streamId);
            if (stream.isDeleted()) {
                throw new StreamDeletedException(streamId);
            }
            if ((expectedVersion != ANY.getNo()) && (stream.getVersion() != expectedVersion)) {
                // Test for idempotency
                final StreamEventsSlice slice = readEventsBackward(streamId, stream.getVersion(),
                        toAppend.size());
                final List<CommonEvent> events = slice.getEvents();
                if (EscSpiUtils.eventsEqual(events, toAppend)) {
                    return stream.getVersion();
                }
                throw new WrongExpectedVersionException(streamId, expectedVersion, stream.getVersion());
            }
        }
        for (int i = 0; i < toAppend.size(); i++) {
            final JpaEvent eventEntry = asJpaEvent(toAppend.get(i));
            getEm().persist(eventEntry);
            final JpaStreamEvent streamEvent = stream.createEvent(streamId, eventEntry);
            getEm().persist(streamEvent);
        }
        return stream.getVersion();

    }

    @Override
    public final void deleteStream(final StreamId streamId, final boolean hardDelete) {
        deleteStream(streamId, ANY.getNo(), hardDelete);
    }

    @Override
    public final void deleteStream(final StreamId streamId, final int expected, final boolean hardDelete) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expected", expected, ExpectedVersion.ANY.getNo());

        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }
        
        final JpaStream stream = findAndLockJpaStream(streamId);
        if (stream == null) {
            // Stream never existed
            if (expected == ExpectedVersion.ANY.getNo()
                    || expected == ExpectedVersion.NO_OR_EMPTY_STREAM.getNo()) {
                if (hardDelete) {
                    final JpaStream newStream = streamFactory.createStream(streamId);
                    newStream.delete(true);
                    getEm().persist(newStream);
                }
                // Ignore
                return;
            }
            throw new WrongExpectedVersionException(streamId, expected, null);
        }
        if (stream.getState() == StreamState.SOFT_DELETED) {
            // Ignore
            return;
        }
        if (stream.getState() == StreamState.HARD_DELETED) {
            throw new StreamDeletedException(streamId);
        }
        // StreamState.ACTIVE
        if (expected != ExpectedVersion.ANY.getNo() && expected != stream.getVersion()) {
            throw new WrongExpectedVersionException(streamId, expected, stream.getVersion());
        }
        stream.delete(hardDelete);

    }

    private JpaStream findAndLockJpaStream(final StreamId streamId) {
        if (!streamEntityExists(streamId)) {
            return null;
        }
        final String sql = createJpqlStreamSelect(streamId);
        LOG.debug("{}", sql);
        final TypedQuery<JpaStream> query = getEm().createQuery(sql, JpaStream.class);
        setJpqlParameters(query, streamId);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        final List<JpaStream> streams = query.getResultList();
        if (streams.size() == 0) {
            return null;
        }
        if (streams.size() == 1) {
            return streams.get(0);
        }
        throw new IllegalStateException("Select returned more than one stream: " + streams.size() + " ["
                + sql + "]");
    }

    private JpaEvent asJpaEvent(final CommonEvent commonEvent) {
        if (commonEvent == null) {
            return null;
        }

        // Serialize data
        final SerializedDataType serDataType = new SerializedDataType(commonEvent.getDataType().asBaseType());
        final SerializedData serData = serialize(serDataType, commonEvent.getData());

        // Serialize meta data
        final SerializedDataType serMetaType;
        if (commonEvent.getMetaType() == null) {
            serMetaType = null;
        } else {
            serMetaType = new SerializedDataType(commonEvent.getMetaType().asBaseType());
        }
        final SerializedData serMeta = serialize(serMetaType, commonEvent.getMeta());

        // Create the JPA event to store
        final JpaData jpaData = new JpaData(serData);
        final JpaData jpaMeta;
        if (serMeta == null) {
            jpaMeta = null;
        } else {
            jpaMeta = new JpaData(serMeta);
        }
        return new JpaEvent(commonEvent.getId(), jpaData, jpaMeta);

    }

}
