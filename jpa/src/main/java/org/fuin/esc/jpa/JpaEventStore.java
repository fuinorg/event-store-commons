/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.fuin.esc.api.*;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerializedData;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.NotThreadSafe;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static org.fuin.esc.api.ExpectedVersion.ANY;

/**
 * JPA Implementation of the event store.
 */
@NotThreadSafe
public final class JpaEventStore extends AbstractJpaEventStore implements EventStore {

    private static final Logger LOG = LoggerFactory.getLogger(JpaEventStore.class);

    private final JpaIdStreamFactory streamFactory;

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
    public JpaEventStore(final EntityManager em, final JpaIdStreamFactory streamFactory,
                         final SerializerRegistry serRegistry, final DeserializerRegistry desRegistry) {
        this(em, streamFactory, serRegistry, desRegistry, JpaTimeouts.DEFAULT);
    }

    /**
     * Constructor with timeouts. Prefer {@link #builder()} when more than the mandatory data is needed.
     *
     * @param em
     *            Entity manager.
     * @param streamFactory
     *            Stream factory.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     * @param timeouts
     *            How long database operations may take.
     */
    public JpaEventStore(final EntityManager em, final JpaIdStreamFactory streamFactory,
                         final SerializerRegistry serRegistry, final DeserializerRegistry desRegistry,
                         final JpaTimeouts timeouts) {
        super(em, serRegistry, desRegistry, timeouts);
        Contract.requireArgNotNull("streamFactory", streamFactory);
        this.streamFactory = streamFactory;
    }

    /**
     * Constructor that additionally up-casts events on read: the deserializer registry is wrapped in an
     * {@link UpcastingDeserializerRegistry} using the given converters, so every event read from this store is
     * lifted from its stored version to the latest in-memory representation (weak-schema across a rolling deploy)
     * without any caller having to wrap the registry itself. An empty {@link ConverterRegistry} is a no-op.
     *
     * @param em
     *            Entity manager.
     * @param streamFactory
     *            Stream factory.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     * @param converters
     *            Registry of version up-casters applied after deserialization.
     */
    public JpaEventStore(final EntityManager em, final JpaIdStreamFactory streamFactory,
                         final SerializerRegistry serRegistry, final DeserializerRegistry desRegistry,
                         final ConverterRegistry converters) {
        this(em, streamFactory, serRegistry, new UpcastingDeserializerRegistry(desRegistry, converters));
    }

    @Override
    public EventStoreCapabilities capabilities() {
        // Relational append/read store: durable and supports hard delete, but no subscriptions or projections.
        return EventStoreCapabilities.builder().hardDelete(true).durablePersistence(true).build();
    }

    @Override
    public void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Do nothing as the operation is not supported
    }

    @Override
    public boolean isSupportsCreateStream() {
        return false;
    }

    @Override
    public long appendToStream(final StreamId streamId, final CommonEvent... events) {
        return appendToStream(streamId, ANY.getNo(), Objects.requireNonNull(EscSpiUtils.asList(events)));
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion,
                               final CommonEvent... events) {
        return appendToStream(streamId, expectedVersion, Objects.requireNonNull(EscSpiUtils.asList(events)));
    }

    @Override
    public long appendToStream(final StreamId streamId, final List<CommonEvent> events) {
        return appendToStream(streamId, ANY.getNo(), events);
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion,
                               final List<CommonEvent> toAppend) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        Contract.requireArgNotNull("toAppend", toAppend);
        ensureOpen();

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
            if (eventEntry != null) {
                getEm().persist(eventEntry);
                final JpaStreamEvent streamEvent = stream.createEvent(streamId, eventEntry);
                getEm().persist(streamEvent);
            }
        }
        return stream.getVersion();

    }

    @Override
    public void deleteStream(final StreamId streamId, final boolean hardDelete) {
        deleteStream(streamId, ANY.getNo(), hardDelete);
    }

    @Override
    public void deleteStream(final StreamId streamId, final long expected, final boolean hardDelete) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expected", expected, ExpectedVersion.ANY.getNo());
        ensureOpen();

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

    @Nullable
    private JpaStream findAndLockJpaStream(final StreamId streamId) {
        if (!streamEntityExists(streamId)) {
            return null;
        }
        final String sql = createJpqlStreamSelect(streamId);
        LOG.debug("{}", sql);
        final TypedQuery<JpaStream> query = getEm().createQuery(sql, JpaStream.class);
        setJpqlParameters(query, streamId);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        // Without a lock timeout this waits forever behind a writer that never commits.
        JpaUtils.withQueryTimeout(getTimeouts(), JpaUtils.withLockTimeout(getTimeouts(), query));
        final List<JpaStream> streams = JpaUtils.execute(query::getResultList);
        if (streams.isEmpty()) {
            return null;
        }
        if (streams.size() == 1) {
            return streams.get(0);
        }
        throw new IllegalStateException("Select returned more than one stream: " + streams.size() + " ["
                + sql + "]");
    }

    @Nullable
    private JpaEvent asJpaEvent(@Nullable final CommonEvent commonEvent) {
        if (commonEvent == null) {
            return null;
        }

        // Serialize data
        final SerializedDataType serDataType = new SerializedDataType(commonEvent.getDataType().asBaseType());
        final SerializedData serData = Objects.requireNonNull(serialize(serDataType, commonEvent.getData()));

        // Serialize meta data
        final SerializedData serMeta;
        if (commonEvent.getMetaType() == null) {
            serMeta = null;
        } else {
            final SerializedDataType serMetaType = new SerializedDataType(commonEvent.getMetaType().asBaseType());
            serMeta = serialize(serMetaType, commonEvent.getMeta());
        }

        // Create the JPA event to store
        final JpaData jpaData = new JpaData(serData);
        final JpaData jpaMeta;
        if (serMeta == null) {
            jpaMeta = null;
        } else {
            jpaMeta = new JpaData(serMeta);
        }
        return new JpaEvent(commonEvent.getId(), commonEvent.getTenantId(), jpaData, jpaMeta,
                new HashSet<>(commonEvent.getCategories()));

    }


    /**
     * Returns a builder for this event store. Preferred over the constructors as soon as more than the
     * mandatory data is needed - the constructors cannot grow further without becoming unreadable.
     *
     * @return New builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a new instance of the event store.
     */
    @SuppressWarnings("NullAway.Init") // Required fields are populated through the builder setters
    public static final class Builder {

        private EntityManager em;

        private JpaIdStreamFactory streamFactory;

        private SerializerRegistry serRegistry;

        private DeserializerRegistry desRegistry;

        @Nullable
        private ConverterRegistry converters;

        @Nullable
        private JpaTimeouts timeouts;

        /**
         * Sets the entity manager.
         *
         * @param em Entity manager.
         * @return Builder.
         */
        public Builder em(final EntityManager em) {
            this.em = em;
            return this;
        }

        /**
         * Sets the stream factory.
         *
         * @param streamFactory Stream factory.
         * @return Builder.
         */
        public Builder streamFactory(final JpaIdStreamFactory streamFactory) {
            this.streamFactory = streamFactory;
            return this;
        }

        /**
         * Sets the serializer registry.
         *
         * @param serRegistry Registry used to locate serializers.
         * @return Builder.
         */
        public Builder serRegistry(final SerializerRegistry serRegistry) {
            this.serRegistry = serRegistry;
            return this;
        }

        /**
         * Sets the deserializer registry.
         *
         * @param desRegistry Registry used to locate deserializers.
         * @return Builder.
         */
        public Builder desRegistry(final DeserializerRegistry desRegistry) {
            this.desRegistry = desRegistry;
            return this;
        }

        /**
         * Sets both types of registries in one call.
         *
         * @param registry Serializer/Deserializer registry to set.
         * @return Builder.
         */
        public Builder serDesRegistry(final SerDeserializerRegistry registry) {
            this.serRegistry = registry;
            this.desRegistry = registry;
            return this;
        }

        /**
         * Sets the version up-caster registry. When set, events read from this store are up-cast from their
         * stored version to the latest in-memory representation.
         *
         * @param converters Registry of version up-casters applied after deserialization.
         * @return Builder.
         */
        public Builder converters(@Nullable final ConverterRegistry converters) {
            this.converters = converters;
            return this;
        }

        /**
         * Sets how long database operations may take. Defaults to {@link JpaTimeouts#DEFAULT}.
         *
         * @param timeouts Query and lock timeouts.
         * @return Builder.
         */
        public Builder timeouts(@Nullable final JpaTimeouts timeouts) {
            this.timeouts = timeouts;
            return this;
        }

        private void verifyNotNull(final String name, @Nullable final Object value) {
            if (value == null) {
                throw new IllegalStateException(
                        "It is mandatory to set the value of '" + name + "' before calling the 'build()' method");
            }
        }

        /**
         * Creates a new instance of the event store from the attributes set via the builder.
         *
         * @return New event store instance.
         */
        public JpaEventStore build() {
            verifyNotNull("em", em);
            verifyNotNull("streamFactory", streamFactory);
            verifyNotNull("serRegistry", serRegistry);
            verifyNotNull("desRegistry", desRegistry);
            final DeserializerRegistry effectiveDesRegistry = converters == null
                    ? desRegistry : new UpcastingDeserializerRegistry(desRegistry, converters);
            return new JpaEventStore(em, streamFactory, serRegistry, effectiveDesRegistry,
                    timeouts == null ? JpaTimeouts.DEFAULT : timeouts);
        }

    }

}
