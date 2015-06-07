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

import static org.fuin.esc.api.EscApiUtils.ANY_VERSION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Credentials;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ReadableEventStoreSync;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamVersionConflictException;
import org.fuin.esc.api.WritableEventStoreSync;
import org.fuin.esc.spi.AbstractDeSerEventStore;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.SerializedData;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.vo.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA Implementation of the event store.
 */
public final class JpaEventStore extends AbstractDeSerEventStore implements
        WritableEventStoreSync, ReadableEventStoreSync {

    private static final Logger LOG = LoggerFactory
            .getLogger(JpaEventStore.class);

    private EntityManager em;

    private JpaIdStreamFactory streamFactory;

    private SerializedDataType serMetaType;

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
     * @param serMetaType
     *            Type used to persist the meta data.
     */
    public JpaEventStore(@NotNull final EntityManager em,
            @NotNull final JpaIdStreamFactory streamFactory,
            @NotNull final SerializerRegistry serRegistry,
            @NotNull final DeserializerRegistry desRegistry,
            @NotNull final SerializedDataType serMetaType) {
        super(serRegistry, desRegistry);
        Contract.requireArgNotNull("em", em);
        Contract.requireArgNotNull("streamFactory", streamFactory);
        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("desRegistry", desRegistry);
        Contract.requireArgNotNull("serMetaType", serMetaType);
        this.em = em;
        this.streamFactory = streamFactory;
        this.serMetaType = serMetaType;
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
    public final int appendToStream(final Optional<Credentials> credentials,
            final StreamId streamId, final CommonEvent... events) {
        return appendToStream(credentials, streamId, ANY_VERSION,
                Arrays.asList(events));
    }

    @Override
    public final int appendToStream(final Optional<Credentials> credentials,
            final StreamId streamId, final int expectedVersion,
            final CommonEvent... events) {
        return appendToStream(credentials, streamId, expectedVersion,
                Arrays.asList(events));
    }

    @Override
    public final int appendToStream(final Optional<Credentials> credentials,
            final StreamId streamId, final List<CommonEvent> events) {
        return appendToStream(credentials, streamId, ANY_VERSION, events);
    }

    @Override
    public final int appendToStream(final Optional<Credentials> credentials,
            final StreamId streamId, final int expectedVersion,
            final List<CommonEvent> events) {

        if (streamId.isProjection()) {
            throw new IllegalArgumentException("Projections are read only: "
                    + streamId);
        }

        final String sql = createStreamSelect(streamId);
        final TypedQuery<JpaStream> query = em
                .createQuery(sql, JpaStream.class);
        setParameters(query, streamId);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        final List<JpaStream> streams = query.getResultList();
        final JpaStream stream;
        if (streams.size() == 0) {
            stream = streamFactory.createStream(streamId);
            em.persist(stream);
        } else {
            stream = streams.get(0);
            if (stream.isDeleted()) {
                throw new StreamDeletedException(streamId);
            }
            if ((expectedVersion != ANY_VERSION)
                    && (stream.getVersion() != expectedVersion)) {
                throw new StreamVersionConflictException(streamId,
                        expectedVersion, stream.getVersion());
            }
        }
        for (int i = 0; i < events.size(); i++) {
            final JpaEvent eventEntry = asJpaEvent(events.get(i));
            em.persist(eventEntry);
            final JpaStreamEvent streamEvent = stream.createEvent(eventEntry);
            em.persist(streamEvent);
        }
        return stream.getVersion();

    }

    @Override
    public final CommonEvent readEvent(final Optional<Credentials> credentials,
            final StreamId streamId, final int eventNumber) {

        final StringBuilder sb = new StringBuilder(createEventSelect(streamId));
        if (streamId.getParameters().size() == 0) {
            sb.append(" WHERE ");
        } else {
            sb.append(" AND ");
        }
        sb.append("s.event_number=:event_number");

        final Query query = em.createNativeQuery(sb.toString(), JpaEvent.class);
        setParameters(query, streamId);

        try {
            final JpaEvent result = (JpaEvent) query.getSingleResult();
            return asCommonEvent(result);
        } catch (final NoResultException ex) {
            throw new EventNotFoundException(streamId, eventNumber);
        }
    }

    @Override
    public final StreamEventsSlice readEventsForward(
            final Optional<Credentials> credentials, final StreamId streamId,
            final int start, final int count) {

        return readStreamEvents(streamId, start, count, true);

    }

    @Override
    public final StreamEventsSlice readEventsBackward(
            final Optional<Credentials> credentials, final StreamId streamId,
            final int start, final int count) {

        return readStreamEvents(streamId, start, count, false);

    }

    @SuppressWarnings("unchecked")
    private StreamEventsSlice readStreamEvents(final StreamId streamId,
            final int start, final int count, final boolean forward)
            throws StreamNotFoundException {

        if (streamId.isProjection()) {
            final JpaProjection projection = em.find(JpaProjection.class,
                    streamId.asString());
            if (projection == null) {
                throw new StreamNotFoundException(streamId);
            }
            if (!projection.isEnabled()) {
                // The projection does exist, but is not ready yet
                return new StreamEventsSlice(start,
                        new ArrayList<CommonEvent>(), start, true);
            }
        }

        // Prepare SQL
        final String sql = createEventSelect(streamId)
                + createOrderBy(streamId, forward);
        LOG.debug(sql);
        final Query query = em.createNativeQuery(sql, JpaEvent.class);
        setParameters(query, streamId);
        query.setFirstResult(start - 1);
        query.setMaxResults(count);

        // Execute query
        final List<JpaEvent> resultList = (List<JpaEvent>) query
                .getResultList();

        // Return result
        final List<CommonEvent> events = asCommonEvents(resultList);
        final int fromEventNumber = start;
        final int nextEventNumber = (start + events.size());
        final boolean endOfStream = (events.size() < count);

        return new StreamEventsSlice(fromEventNumber, events, nextEventNumber,
                endOfStream);
    }

    private String createOrderBy(final StreamId streamId, final boolean asc) {
        final StringBuilder sb = new StringBuilder(" ORDER BY ");
        sb.append("ev.id");
        if (asc) {
            sb.append(" ASC");
        } else {
            sb.append(" DESC");
        }
        return sb.toString();
    }

    @Override
    public final void deleteStream(final Optional<Credentials> credentials,
            final StreamId streamId) {
        deleteStream(credentials, streamId, ANY_VERSION);
    }

    @Override
    public final void deleteStream(final Optional<Credentials> credentials,
            final StreamId streamId, final int expectedVersion) {

        final JpaStream stream = em.find(JpaStream.class, streamId.getName(),
                LockModeType.PESSIMISTIC_WRITE);
        if (stream == null) {
            throw new StreamNotFoundException(streamId);
        }
        if (stream.isDeleted()) {
            throw new StreamDeletedException(streamId);
        }
        if (stream.getVersion() != expectedVersion) {
            throw new StreamVersionConflictException(streamId, expectedVersion,
                    stream.getVersion());
        }
        stream.delete();

    }

    private String createStreamSelect(final StreamId streamId) {
        final List<KeyValue> params = streamId.getParameters();
        final StringBuilder sb = new StringBuilder("SELECT t FROM "
                + streamId.getName() + "Stream t");
        if (params.size() > 0) {
            sb.append(" WHERE ");
            for (int i = 0; i < params.size(); i++) {
                final KeyValue param = params.get(i);
                if (i > 0) {
                    sb.append(" AND ");
                }
                sb.append("t." + param.getKey() + "=:" + param.getKey());
            }
        }
        return sb.toString();
    }

    private String createEventSelect(final StreamId streamId) {
        final List<KeyValue> params = streamId.getParameters();
        final StringBuilder sb = new StringBuilder(
                "SELECT ev.* FROM events ev, " + sqlName(streamId.getName())
                        + "_events" + " s WHERE ev.id=s.events_id");
        if (params.size() > 0) {
            for (int i = 0; i < params.size(); i++) {
                final KeyValue param = params.get(i);
                sb.append(" AND ");
                sb.append("s." + sqlName(param.getKey()) + "=:"
                        + param.getKey());
            }
        }
        return sb.toString();
    }

    private String sqlName(final String name) {
        return name.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
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

    private List<CommonEvent> asCommonEvents(final List<JpaEvent> eventEntries) {
        final List<CommonEvent> events = new ArrayList<CommonEvent>();
        for (JpaEvent eventEntry : eventEntries) {
            events.add(asCommonEvent(eventEntry));
        }
        return events;
    }

    private CommonEvent asCommonEvent(final JpaEvent jpaEvent) {
        final Object data = deserialize(jpaEvent.getData());
        final Object meta = deserialize(jpaEvent.getMeta());
        return new CommonEvent(jpaEvent.getEventId(), jpaEvent.getData()
                .getType(), data, meta);
    }

    private JpaEvent asJpaEvent(final CommonEvent commonEvent) {
        if (commonEvent == null) {
            return null;
        }

        // Serialize data
        final SerializedDataType serDataType = new SerializedDataType(
                commonEvent.getType().asBaseType());
        final SerializedData serData = serialize(serDataType,
                commonEvent.getData());

        // Serialize meta data
        final SerializedData serMeta = serialize(serMetaType,
                commonEvent.getMeta());

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

    private Object deserialize(final JpaData data) {
        if (data == null) {
            return null;
        }
        final SerializedData serializedData = new SerializedData(
                new SerializedDataType(data.getType().asBaseType()),
                data.getMimeType(), data.getRaw());
        return deserialize(serializedData);
    }

}
