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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ReadableEventStoreSync;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerializedData;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.vo.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read only JPA implementation of the event store.
 */
public abstract class AbstractJpaEventStore implements ReadableEventStoreSync {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractJpaEventStore.class);

    private final EntityManager em;

    private final SerializerRegistry serRegistry;

    private final DeserializerRegistry desRegistry;

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
    public final void open() {
        // Do nothing
    }

    @Override
    public final void close() {
        // Do nothing
    }

    @Override
    public final CommonEvent readEvent(final StreamId streamId, final int eventNumber) {

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
    public final StreamEventsSlice readEventsForward(final StreamId streamId, final int start, final int count) {

        return readStreamEvents(streamId, start, count, true);

    }

    @Override
    public final StreamEventsSlice readEventsBackward(final StreamId streamId, final int start,
            final int count) {

        return readStreamEvents(streamId, start, count, false);

    }

    @SuppressWarnings("unchecked")
    private StreamEventsSlice readStreamEvents(final StreamId streamId, final int start, final int count,
            final boolean forward) throws StreamNotFoundException {

        if (streamId.isProjection()) {
            final JpaProjection projection = em.find(JpaProjection.class, streamId.asString());
            if (projection == null) {
                throw new StreamNotFoundException(streamId);
            }
            if (!projection.isEnabled()) {
                // The projection does exist, but is not ready yet
                return new StreamEventsSlice(start, new ArrayList<CommonEvent>(), start, true);
            }
        }

        // Prepare SQL
        final String sql = createEventSelect(streamId) + createOrderBy(streamId, forward);
        LOG.debug(sql);
        final Query query = em.createNativeQuery(sql, JpaEvent.class);
        setParameters(query, streamId);
        query.setFirstResult(start - 1);
        query.setMaxResults(count);

        // Execute query
        final List<JpaEvent> resultList = (List<JpaEvent>) query.getResultList();

        // Return result
        final List<CommonEvent> events = asCommonEvents(resultList);
        final int fromEventNumber = start;
        final int nextEventNumber = (start + events.size());
        final boolean endOfStream = (events.size() < count);

        return new StreamEventsSlice(fromEventNumber, events, nextEventNumber, endOfStream);
    }

    @Override
    public final boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);

        final JpaStream stream = getEm().find(JpaStream.class, streamId.getName());
        return (stream != null);

    }
    
    @Override
    public final StreamState streamState(final StreamId streamId) {
        final JpaStream stream = getEm().find(JpaStream.class, streamId.getName());
        if (stream == null) {
            throw new StreamNotFoundException(streamId);
        }
        return stream.getState();
    }

    /**
     * Tries to find a serializer for the given type of object and converts it into a storable data block.
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

    private String createEventSelect(final StreamId streamId) {
        final List<KeyValue> params = streamId.getParameters();
        final StringBuilder sb = new StringBuilder("SELECT ev.* FROM events ev, " + nativeTableName(streamId)
                + " s WHERE ev.id=s.events_id");
        if (params.size() > 0) {
            for (int i = 0; i < params.size(); i++) {
                final KeyValue param = params.get(i);
                sb.append(" AND ");
                sb.append("s." + sqlName(param.getKey()) + "=:" + param.getKey());
            }
        }
        return sb.toString();
    }

    private String nativeTableName(final StreamId streamId) {
        if (streamId.isProjection()) {
            return sqlName(streamId.getName());
        }
        return sqlName(streamId.getName()) + "_events";
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
        return new SimpleCommonEvent(jpaEvent.getEventId(), jpaEvent.getData().getType(), data, meta);
    }

    private Object deserialize(final JpaData data) {
        if (data == null) {
            return null;
        }
        final SerializedData serializedData = new SerializedData(new SerializedDataType(data.getType()
                .asBaseType()), data.getMimeType(), data.getRaw());
        return EscSpiUtils.deserialize(desRegistry, serializedData);
    }

}
