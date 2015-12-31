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
package org.fuin.esc.jpa;

import static org.fuin.esc.api.ExpectedVersion.ANY;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerializedData;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.vo.KeyValue;

/**
 * JPA Implementation of the event store.
 */
public final class JpaEventStore extends AbstractJpaEventStore implements EventStoreSync {

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

        final String sql = createStreamSelect(streamId);
        final TypedQuery<JpaStream> query = getEm().createQuery(sql, JpaStream.class);
        setParameters(query, streamId);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        final List<JpaStream> streams = query.getResultList();
        final JpaStream stream;
        if (streams.size() == 0) {
            stream = streamFactory.createStream(streamId);
            getEm().persist(stream);
        } else {
            stream = streams.get(0);
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
            final JpaStreamEvent streamEvent = stream.createEvent(eventEntry);
            getEm().persist(streamEvent);
        }
        return stream.getVersion();

    }

    @Override
    public final void deleteStream(final StreamId streamId, final boolean hardDelete) {
        deleteStream(streamId, ANY.getNo(), hardDelete);
    }

    @Override
    public final void deleteStream(final StreamId streamId, final int expectedVersion,
            final boolean hardDelete) {

        final JpaStream stream = getEm().find(JpaStream.class, streamId.getName(),
                LockModeType.PESSIMISTIC_WRITE);
        if (stream == null) {
            throw new StreamNotFoundException(streamId);
        }
        if (stream.isDeleted()) {
            throw new StreamDeletedException(streamId);
        }
        if (stream.getVersion() != expectedVersion) {
            throw new WrongExpectedVersionException(streamId, expectedVersion, stream.getVersion());
        }
        stream.delete(hardDelete);

    }

    private void setParameters(final Query query, final StreamId streamId) {
        final List<KeyValue> params = streamId.getParameters();
        if (params.size() > 0) {
            for (int i = 0; i < params.size(); i++) {
                final KeyValue param = params.get(i);
                query.setParameter(param.getKey(), param.getValue());
            }
        }
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
