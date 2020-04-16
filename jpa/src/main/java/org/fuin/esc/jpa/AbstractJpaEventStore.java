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

import static org.fuin.esc.jpa.JpaUtils.camel2Underscore;
import static org.fuin.esc.jpa.JpaUtils.nativeEventsTableName;
import static org.fuin.esc.jpa.JpaUtils.streamEntityName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.EntityType;
import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ReadableEventStore;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.spi.AbstractReadableEventStore;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerializedData;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.ConstraintViolationException;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.vo.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read only JPA implementation of the event store.
 */
public abstract class AbstractJpaEventStore extends AbstractReadableEventStore implements ReadableEventStore {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractJpaEventStore.class);

    private static final String JPA_EVENT_PREFIX = "ev";

    private static final String JPA_STREAM_EVENT_PREFIX = "se";

    private final EntityManager em;

    private final SerializerRegistry serRegistry;

    private final DeserializerRegistry desRegistry;

    private boolean open;

    /**
     * Constructor with all mandatory data.
     * 
     * @param em
     *            Entity manager.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     */
    public AbstractJpaEventStore(@NotNull final EntityManager em,
            @NotNull final SerializerRegistry serRegistry, @NotNull final DeserializerRegistry desRegistry) {
        super();
        Contract.requireArgNotNull("em", em);
        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("desRegistry", desRegistry);
        this.em = em;
        this.serRegistry = serRegistry;
        this.desRegistry = desRegistry;
        this.open = false;
    }

    /**
     * Returns the entity manager.
     * 
     * @return Entity manager.
     */
    protected final EntityManager getEm() {
        return em;
    }

    /**
     * Returns a registry of serializers.
     * 
     * @return Registry with known serializers.
     */
    @NotNull
    protected final SerializerRegistry getSerializerRegistry() {
        return serRegistry;
    }

    /**
     * Returns a registry of deserializers.
     * 
     * @return Registry with known deserializers.
     */
    @NotNull
    protected final DeserializerRegistry getDeserializerRegistry() {
        return desRegistry;
    }

    @Override
    public final AbstractJpaEventStore open() {
        if (open) {
            // Ignore
            return this;
        }
        this.open = true;
        return this;
    }

    @Override
    public final void close() {
        if (!open) {
            // Ignore
            return;
        }
        this.open = false;
    }

    @Override
    public final CommonEvent readEvent(final StreamId streamId, final long eventNumber) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);
        ensureOpen();
        verifyStreamEntityExists(streamId);

        final NativeSqlCondition eventNo = new NativeSqlCondition(JpaStreamEvent.COLUMN_EVENT_NUMBER, "=",
                eventNumber);
        final List<NativeSqlCondition> conditions = createNativeSqlConditions(streamId, eventNo);

        final String nativeSql = createNativeSqlEventSelect(streamId, conditions);

        final Query query = em.createNativeQuery(nativeSql, JpaEvent.class);
        setNativeSqlParameters(query, conditions);

        try {
            final JpaEvent result = (JpaEvent) query.getSingleResult();
            return asCommonEvent(result);
        } catch (final NoResultException ex) {
            throw new EventNotFoundException(streamId, eventNumber);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final StreamEventsSlice readEventsForward(final StreamId streamId, final long start,
            final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();
        verifyStreamEntityExists(streamId);

        if (streamId.isProjection()) {
            final JpaProjection projection = em.find(JpaProjection.class, streamEntityName(streamId));
            if (projection == null) {
                throw new StreamNotFoundException(streamId);
            }
            if (!projection.isEnabled()) {
                // The projection does exist, but is not ready yet
                return new StreamEventsSlice(start, new ArrayList<CommonEvent>(), start, true);
            }
        } else {
            final JpaStream stream = findStream(streamId);
            if (stream.getState() == StreamState.HARD_DELETED) {
                throw new StreamDeletedException(streamId);
            }
        }

        // Prepare SQL
        final NativeSqlCondition greaterOrEqualEventNumber = new NativeSqlCondition(JPA_STREAM_EVENT_PREFIX,
                JpaStreamEvent.COLUMN_EVENT_NUMBER, ">=", start);
        final List<NativeSqlCondition> conditions = createNativeSqlConditions(streamId,
                greaterOrEqualEventNumber);
        final String sql = createNativeSqlEventSelect(streamId, conditions) + createOrderBy(streamId, true);
        LOG.debug(sql);
        final Query query = em.createNativeQuery(sql, JpaEvent.class);
        setNativeSqlParameters(query, conditions);
        query.setMaxResults(count);
        final List<JpaEvent> resultList = query.getResultList();

        // Return result
        final List<CommonEvent> events = asCommonEvents(resultList);
        final long fromEventNumber = start;
        final long nextEventNumber = (start + events.size());
        final boolean endOfStream = (events.size() < count);

        return new StreamEventsSlice(fromEventNumber, events, nextEventNumber, endOfStream);

    }

    @SuppressWarnings("unchecked")
    @Override
    public final StreamEventsSlice readEventsBackward(final StreamId streamId, final long start,
            final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();
        verifyStreamEntityExists(streamId);

        if (streamId.isProjection()) {
            final JpaProjection projection = em.find(JpaProjection.class, streamEntityName(streamId));
            if (projection == null) {
                throw new StreamNotFoundException(streamId);
            }
            if (!projection.isEnabled()) {
                // The projection does exist, but is not ready yet
                return new StreamEventsSlice(start, new ArrayList<CommonEvent>(), start, true);
            }
        } else {
            final JpaStream stream = findStream(streamId);
            if (stream.getState() == StreamState.HARD_DELETED) {
                throw new StreamDeletedException(streamId);
            }
        }

        // Prepare SQL
        final NativeSqlCondition greaterOrEqualEventNumber = new NativeSqlCondition(JPA_STREAM_EVENT_PREFIX,
                JpaStreamEvent.COLUMN_EVENT_NUMBER, "<=", start);
        final List<NativeSqlCondition> conditions = createNativeSqlConditions(streamId,
                greaterOrEqualEventNumber);
        final String sql = createNativeSqlEventSelect(streamId, conditions) + createOrderBy(streamId, false);
        LOG.debug(sql);
        final Query query = em.createNativeQuery(sql, JpaEvent.class);
        setNativeSqlParameters(query, conditions);
        query.setMaxResults(count);
        final List<JpaEvent> resultList = query.getResultList();

        // Return result
        final List<CommonEvent> events = asCommonEvents(resultList);
        final long fromEventNumber = start;
        long nextEventNumber = start - resultList.size();
        if (nextEventNumber < 0) {
            nextEventNumber = 0;
        }
        final boolean endOfStream = (start - count) < 0;

        return new StreamEventsSlice(fromEventNumber, events, nextEventNumber, endOfStream);

    }

    @Override
    public final boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();
        if (!streamEntityExists(streamId)) {
            return false;
        }

        final String sql = createJpqlStreamSelect(streamId);
        final TypedQuery<JpaStream> query = getEm().createQuery(sql, JpaStream.class);
        setJpqlParameters(query, streamId);
        final List<JpaStream> streams = query.getResultList();
        if (streams.size() == 0) {
            return false;
        }
        if (streams.size() == 1) {
            final JpaStream stream = streams.get(0);
            return (stream.getState() == StreamState.ACTIVE);
        }
        throw new IllegalStateException(
                "Select returned more than one stream: " + streams.size() + " [" + sql + "]");

    }

    @Override
    public final StreamState streamState(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final JpaStream stream = findStream(streamId);
        return stream.getState();

    }

    /**
     * Verifies if a stream entity exists or throws an
     * {@link StreamNotFoundException} otherwise.
     * 
     * @param streamId
     *            Stream to test.
     */
    protected final void verifyStreamEntityExists(final StreamId streamId) {
        if (!streamEntityExists(streamId)) {
            throw new StreamNotFoundException(streamId);
        }
    }

    /**
     * Returns if a stream entity exists.
     * 
     * @param streamId
     *            Stream to test.
     * 
     * @return TRUE if the entity is known, else FALSE.
     */
    protected final boolean streamEntityExists(final StreamId streamId) {
        return entityExists(streamEntityName(streamId));
    }

    /**
     * Returns if an entity with agiven name exists.
     * 
     * @param entityName
     *            Entity to test.
     * 
     * @return TRUE if the entity is known, else FALSE.
     */
    protected final boolean entityExists(final String entityName) {
        final Set<EntityType<?>> entityTypes = getEm().getMetamodel().getEntities();
        for (final EntityType<?> entityType : entityTypes) {
            if (entityType.getName().equals(entityName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to find a serializer for the given type of object and converts it
     * into a storable data block.
     * 
     * @param type
     *            Type of event.
     * @param data
     *            Event of the given type.
     * 
     * @return Event ready to persist.
     */
    protected final SerializedData serialize(final SerializedDataType type, final Object data) {
        return EscSpiUtils.serialize(serRegistry, type, data);
    }

    /**
     * Tries to find a deserializer for the given data block.
     * 
     * @param data
     *            Persisted data.
     * 
     * @return Unmarshalled event.
     * 
     * @param <T>
     *            Expected type of event.
     */
    protected final <T> T deserialize(final SerializedData data) {
        return EscSpiUtils.deserialize(desRegistry, data);
    }

    /**
     * Creates the JPQL to select the stream itself.
     * 
     * @param streamId
     *            Unique stream identifier.
     * 
     * @return JPQL that selects the stream with the given identifier.
     */
    protected final String createJpqlStreamSelect(final StreamId streamId) {

        if (streamId.isProjection()) {
            throw new IllegalArgumentException("Projections do not have a stream table : " + streamId);
        }

        final List<KeyValue> params = new ArrayList<>(streamId.getParameters());
        if (params.size() == 0) {
            // NoParamsStream
            params.add(new KeyValue("streamName", streamId.getName()));
        }
        final StringBuilder sb = new StringBuilder("SELECT t FROM " + streamEntityName(streamId) + " t");
        sb.append(" WHERE ");
        for (int i = 0; i < params.size(); i++) {
            final KeyValue param = params.get(i);
            if (i > 0) {
                sb.append(" AND ");
            }
            sb.append("t." + param.getKey() + "=:" + param.getKey());
        }
        return sb.toString();
    }

    /**
     * Reads the stream with the given identifier from the DB and returns it.
     * 
     * @param streamId
     *            Stream to load.
     * 
     * @return Stream.
     */
    @NotNull
    protected final JpaStream findStream(@NotNull final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        verifyStreamEntityExists(streamId);

        final String sql = createJpqlStreamSelect(streamId);
        final TypedQuery<JpaStream> query = getEm().createQuery(sql, JpaStream.class);
        setJpqlParameters(query, streamId);
        final List<JpaStream> streams = query.getResultList();
        if (streams.size() == 0) {
            throw new StreamNotFoundException(streamId);
        }
        final JpaStream stream = streams.get(0);
        if (stream.getState() == StreamState.SOFT_DELETED) {
            // TODO Remove after event store has a way to distinguish between
            // never-existing and soft deleted
            // streams
            throw new StreamNotFoundException(streamId);
        }
        return stream;

    }

    /**
     * Sets parameters in a query.
     * 
     * @param query
     *            Query to set parameters for.
     * @param streamId
     *            Unique stream identifier that has the parameter values.
     */
    protected final void setJpqlParameters(final Query query, final StreamId streamId) {
        final List<KeyValue> params = new ArrayList<>(streamId.getParameters());
        if (params.size() == 0) {
            params.add(new KeyValue("streamName", streamId.getName()));
        }
        for (int i = 0; i < params.size(); i++) {
            final KeyValue param = params.get(i);
            query.setParameter(param.getKey(), param.getValue());
        }
    }

    /**
     * Sets parameters in a query.
     * 
     * @param query
     *            Query to set parameters for.
     * @param streamId
     *            Unique stream identifier that has the parameter values.
     * @param additionalConditions
     *            Parameters to add in addition to the ones from the stream
     *            identifier.
     */
    private final void setNativeSqlParameters(final Query query, final List<NativeSqlCondition> conditions) {
        for (final NativeSqlCondition condition : conditions) {
            query.setParameter(condition.getColumn(), condition.getValue());
        }
    }

    /**
     * Creates a native SQL select using the parameters from the stream
     * identifier and optional other arguments.
     * 
     * @param streamId
     *            Unique stream identifier that has the parameter values.
     * @param additionalParams
     *            Parameters to add in addition to the ones from the stream
     *            identifier.
     * 
     * @return JPQL for selecting the events.
     */
    private final String createNativeSqlEventSelect(final StreamId streamId,
            final List<NativeSqlCondition> conditions) {

        final StringBuilder sb = new StringBuilder("SELECT " + JPA_EVENT_PREFIX + ".* FROM "
                + JpaEvent.TABLE_NAME + " " + JPA_EVENT_PREFIX + ", " + nativeEventsTableName(streamId) + " "
                + JPA_STREAM_EVENT_PREFIX + " WHERE " + JPA_EVENT_PREFIX + "." + JpaEvent.COLUMN_ID + "="
                + JPA_STREAM_EVENT_PREFIX + "." + JpaStreamEvent.COLUMN_EVENTS_ID);
        for (final NativeSqlCondition condition : conditions) {
            sb.append(" AND ");
            sb.append(condition.asWhereConditionWithParam());
        }
        return sb.toString();
    }

    private String createOrderBy(final StreamId streamId, final boolean asc) {
        final StringBuilder sb = new StringBuilder(" ORDER BY ");
        sb.append(JPA_STREAM_EVENT_PREFIX + "." + JpaStreamEvent.COLUMN_EVENT_NUMBER);
        if (asc) {
            sb.append(" ASC");
        } else {
            sb.append(" DESC");
        }
        return sb.toString();
    }

    private List<NativeSqlCondition> createNativeSqlConditions(final StreamId streamId,
            final NativeSqlCondition... additionalConditions) {
        final List<NativeSqlCondition> conditions;
        if (additionalConditions == null) {
            conditions = new ArrayList<>();
        } else {
            conditions = new ArrayList<>(Arrays.asList(additionalConditions));
        }
        if (streamId.getParameters().size() == 0) {
            conditions.add(new NativeSqlCondition(JPA_STREAM_EVENT_PREFIX, NoParamsEvent.COLUMN_STREAM_NAME,
                    "=", streamId.getName()));
        } else {
            for (final KeyValue kv : streamId.getParameters()) {
                conditions.add(new NativeSqlCondition(camel2Underscore(kv.getKey()), "=", kv.getValue()));
            }
        }
        return conditions;
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
        if (meta == null) {
            return new SimpleCommonEvent(jpaEvent.getEventId(), jpaEvent.getData().getTypeName(), data);
        }
        return new SimpleCommonEvent(jpaEvent.getEventId(), jpaEvent.getData().getTypeName(), data,
                jpaEvent.getMeta().getTypeName(), meta);
    }

    private Object deserialize(final JpaData data) {
        if (data == null) {
            return null;
        }
        final SerializedData serializedData = new SerializedData(
                new SerializedDataType(data.getTypeName().asBaseType()), data.getMimeType(), data.getRaw());
        return EscSpiUtils.deserialize(desRegistry, serializedData);
    }

    /**
     * Makes sure the event store was opened before or throws a
     * {@link ConstraintViolationException} otherwise.
     */
    protected final void ensureOpen() {
        if (!open) {
            open();
        }
    }

}
