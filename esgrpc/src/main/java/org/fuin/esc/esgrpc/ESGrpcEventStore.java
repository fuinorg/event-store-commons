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
package org.fuin.esc.esgrpc;

import io.grpc.StatusRuntimeException;
import io.kurrent.dbclient.*;
import org.fuin.esc.api.*;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.esc.spi.AbstractReadableEventStore;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.utils4j.TestOmitted;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.fuin.esc.api.ExpectedVersion.ANY;

/**
 * Implementation that connects to the event store (<a href="http://www.geteventstore.com">Eventstore</a>) using the GRPC API.
 */
@ThreadSafe
@TestOmitted("Tested in the 'test' project")
public final class ESGrpcEventStore extends AbstractReadableEventStore implements IESGrpcEventStore {

    private final KurrentDBClient es;

    private final ESGrpcEventStoreSupport support;

    private final Duration callTimeout;

    /**
     * Private constructor with all data used by the builder.
     *
     * @param es                Connection that is maintained outside. Opening/Closing is up to the caller!
     * @param serRegistry       Registry used to locate serializers.
     * @param desRegistry       Registry used to locate deserializers.
     * @param baseTypeFactory   Factory used to create basic types.
     * @param targetContentType Target content type (Allows only 'application/xml'
     *                          or 'application/json' with 'utf-8' encoding).
     * @param tenantContext     Provides the current tenant.
     * @param callTimeout       Maximum time to wait for a single event store call.
     */
    private ESGrpcEventStore(final KurrentDBClient es,
                             final SerializerRegistry serRegistry,
                             final DeserializerRegistry desRegistry,
                             final IBaseTypeFactory baseTypeFactory,
                             final EnhancedMimeType targetContentType,
                             final TenantContext tenantContext,
                             final Duration callTimeout) {
        super();
        Contract.requireArgNotNull("es", es);
        this.es = es;
        this.callTimeout = callTimeout;
        this.support = new ESGrpcEventStoreSupport(serRegistry, desRegistry, baseTypeFactory,
                targetContentType, tenantContext);
    }

    private void ensureOpen() {
        if (es.isShutdown()) {
            throw new IllegalStateException("The event store has already been closed");
        }
    }

    @Override
    public ESGrpcEventStore open() {
        // Do nothing - We assume that the connection is already
        // fully initialized when passed in to constructor
        return this;
    }

    @Override
    public void close() {
        // Do nothing - Connection is handled outside
    }

    @Override
    public EventStoreCapabilities capabilities() {
        return ESGrpcCapabilities.INSTANCE;
    }

    @Override
    public boolean isSupportsCreateStream() {
        return false;
    }

    @Override
    public void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Do nothing as the operation is not supported
    }

    @Override
    public long appendToStream(final StreamId streamId, final CommonEvent... events)
            throws StreamNotFoundException, StreamDeletedException, StreamReadOnlyException {
        return appendToStream(streamId, -2, Objects.requireNonNull(EscSpiUtils.asList(events)));
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events)
            throws StreamNotFoundException, StreamDeletedException, WrongExpectedVersionException,
            StreamReadOnlyException {
        return appendToStream(streamId, expectedVersion, Objects.requireNonNull(EscSpiUtils.asList(events)));
    }

    @Override
    public long appendToStream(final StreamId streamId, final List<CommonEvent> events)
            throws StreamNotFoundException, StreamDeletedException, StreamReadOnlyException {
        return appendToStream(streamId, -2, events);
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion,
                               final List<CommonEvent> commonEvents)
            throws StreamDeletedException, WrongExpectedVersionException, StreamReadOnlyException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        Contract.requireArgNotNull("commonEvents", commonEvents);
        ESGrpcEventStoreSupport.ensureStreamNoProjection(streamId);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);

        try {
            final Iterator<EventData> eventDataIt = support.asEventData(commonEvents).iterator();
            final WriteResult result = GrpcCalls.await(es.appendToStream(sid.asString(),
                    AppendToStreamOptions.get().streamState(ESGrpcEventStoreSupport.version2State(expectedVersion)),
                    eventDataIt), callTimeout, "appendToStream");
            return result.getNextExpectedRevision().toRawLong();
        } catch (final ExecutionException ex) {
            throw ESGrpcEventStoreSupport.mapException(ex.getCause(), sid, expectedVersion);
        } catch (InterruptedException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for appendToStream(..) result", ex);
        }

    }

    @Override
    public void deleteStream(final StreamId streamId, final long expectedVersion, final boolean hardDelete)
            throws StreamDeletedException, WrongExpectedVersionException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        ESGrpcEventStoreSupport.ensureStreamNoProjection(streamId);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        try {
            final DeleteStreamOptions options = DeleteStreamOptions.get()
                    .streamState(ESGrpcEventStoreSupport.version2State(expectedVersion));
            if (hardDelete) {
                GrpcCalls.await(es.tombstoneStream(sid.asString(), options), callTimeout, "tombstoneStream");
            } else {
                GrpcCalls.await(es.deleteStream(sid.asString(), options), callTimeout, "deleteStream");
            }
        } catch (final ExecutionException ex) {
            throw ESGrpcEventStoreSupport.mapException(ex.getCause(), sid, expectedVersion);
        } catch (final InterruptedException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for deleteStream(..) result", ex);
        }

    }

    @Override
    public void deleteStream(final StreamId streamId, final boolean hardDelete)
            throws StreamNotFoundException, StreamDeletedException {

        deleteStream(streamId, ANY.getNo(), hardDelete);

    }

    @Override
    public StreamEventsSlice readEventsForward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        try {

            final ReadStreamOptions options = ReadStreamOptions.get().forwards().fromRevision(start).maxCount(count)
                    .resolveLinkTos();

            final ReadResult readResult = GrpcCalls.await(es.readStream(sid.asString(), options), callTimeout, "readEventsForward");
            final List<ResolvedEvent> resolvedEvents = readResult.getEvents();
            final List<CommonEvent> events = support.asCommonEvents(resolvedEvents);
            // The cursor and the end-of-stream flag count what the server returned, NOT what survived the
            // conversion. asCommonEvents drops links whose target is gone, and deriving the next event
            // number from the shorter list would hand back a position already read - the reader would
            // fetch the same slice forever - while a short slice would look like the end of the stream.
            final int returned = resolvedEvents.size();
            final boolean endOfStream = count > returned;
            return new StreamEventsSlice(start, events, start + returned, endOfStream);
        } catch (ExecutionException ex) {
            throw ESGrpcEventStoreSupport.mapException(ex.getCause(), sid, ANY.getNo());
        } catch (InterruptedException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for readEventsForward(..) result", ex);
        }

    }

    @Override
    public StreamEventsSlice readEventsBackward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        try {
            final ReadStreamOptions options = ReadStreamOptions.get().backwards().fromRevision(start).maxCount(count)
                    .resolveLinkTos();
            final ReadResult slice = GrpcCalls.await(es.readStream(sid.asString(), options), callTimeout, "readEventsBackward");
            final List<ResolvedEvent> resolvedEvents = slice.getEvents();
            final List<CommonEvent> events = support.asCommonEvents(resolvedEvents);
            // Counts what the server returned rather than what survived the conversion - see the forward
            // read for why.
            long nextEventNumber = start - resolvedEvents.size();
            final boolean endOfStream = (start - count < 0);
            if (endOfStream) {
                nextEventNumber = 0;
            }
            return new StreamEventsSlice(start, events, nextEventNumber, endOfStream);
        } catch (ExecutionException ex) {
            throw ESGrpcEventStoreSupport.mapException(ex.getCause(), sid, ANY.getNo());
        } catch (InterruptedException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for readEventsBackward(..) result", ex);
        }

    }

    @Override
    public CommonEvent readEvent(final StreamId streamId, final long eventNumber) {
        final StreamEventsSlice slice = readEventsForward(streamId, eventNumber, 1);
        if (slice.getEvents().isEmpty()) {
            throw new EventNotFoundException(streamId, eventNumber);
        }
        return slice.getEvents().get(0);
    }

    @Override
    public boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        try {
            final ReadStreamOptions options = ReadStreamOptions.get().forwards().fromRevision(0).maxCount(1);
            GrpcCalls.await(es.readStream(sid.asString(), options), callTimeout, "streamExists");
            return true;
        } catch (ExecutionException ex) {
            // Only "not found" means the stream does not exist. A connectivity failure must NOT be
            // reported as "false" - that would silently turn "no answer" into a business answer.
            if (ex.getCause() instanceof io.kurrent.dbclient.StreamNotFoundException
                    || ESGrpcEventStoreSupport.statusIsNotFound(ex.getCause())
                    // A hard deleted stream does not exist any more either.
                    || ESGrpcEventStoreSupport.statusIsDeleted(ex.getCause())) {
                return false;
            }
            if (ESGrpcEventStoreSupport.statusIsConnectivityProblem(ex.getCause())) {
                throw new EscConnectionException(
                        "Could not reach the event store executing streamExists(..) on stream '" + sid + "'",
                        ex.getCause());
            }
            throw new RuntimeException("Error executing streamExists(..)", ex);
        } catch (InterruptedException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for streamExists(..) result", ex);
        }

    }

    @Override
    public StreamState streamState(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        try {
            GrpcCalls.await(es.readStream(sid.asString(), ReadStreamOptions.get().forwards().fromRevision(0)),
                    callTimeout, "streamState");
            return StreamState.ACTIVE;
        } catch (ExecutionException ex) {
            if (ESGrpcEventStoreSupport.statusIsDeleted(ex.getCause())) {
                return StreamState.HARD_DELETED;
            }
            if (ex.getCause() instanceof io.kurrent.dbclient.StreamNotFoundException) {
                return softDeleted(streamId);
            }
            throw new RuntimeException("Error executing streamState(..)", ex);
        } catch (InterruptedException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for streamState(..) result", ex);
        }

    }

    private StreamState softDeleted(final StreamId streamId) {
        // Workaround for reading metadata because of:
        // https://github.com/EventStore/KurrentDB-Client-Java/issues/240
        try {
            GrpcCalls.await(es.readStream(ESGrpcEventStoreSupport.metaStreamName(streamId),
                    ReadStreamOptions.get().forwards().fromRevision(0)), callTimeout, "readStreamMetaData");
            throw new StreamNotFoundException(streamId);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof io.kurrent.dbclient.StreamNotFoundException) {
                throw new StreamNotFoundException(streamId);
            }
            throw new RuntimeException("Error reading stream meta data", ex);
        } catch (InterruptedException ex) { // NOSONAR
            throw new RuntimeException("Error reading stream status", ex);
        }
    }

    /**
     * Builder used to create a new instance of the event store.
     */
    @SuppressWarnings("NullAway.Init") // Required fields are populated through the builder setters
    public static final class Builder {

        private io.kurrent.dbclient.KurrentDBClient eventStore;

        private SerializerRegistry serRegistry;

        private DeserializerRegistry desRegistry;

        @Nullable
        private ConverterRegistry converters;

        private IBaseTypeFactory baseTypeFactory;

        private EnhancedMimeType targetContentType;

        @Nullable
        private TenantContext tenantContext;

        @Nullable
        private Duration callTimeout;

        /**
         * Sets the event store to use internally.
         *
         * @param eventStore TCP/IP event store connection.
         * @return Builder.
         */
        public Builder eventStore(io.kurrent.dbclient.KurrentDBClient eventStore) {
            this.eventStore = eventStore;
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
         * stored version to the latest in-memory representation (the deserializer registry is wrapped in an
         * {@link UpcastingDeserializerRegistry}); {@literal null} or an empty registry leaves reads unchanged.
         *
         * @param converters Registry of version up-casters applied after deserialization.
         * @return Builder.
         */
        public Builder converters(@Nullable final ConverterRegistry converters) {
            this.converters = converters;
            return this;
        }

        /**
         * Sets the base type factory.
         *
         * @param baseTypeFactory Factory used to create base types.
         * @return Builder.
         */
        public Builder baseTypeFactory(final IBaseTypeFactory baseTypeFactory) {
            this.baseTypeFactory = baseTypeFactory;
            return this;
        }


        /**
         * Sets the target content type.
         *
         * @param targetContentType Target content type (Allows only 'application/xml'
         *                          or 'application/json' with 'utf-8' encoding).
         * @return Builder.
         */
        public Builder targetContentType(final EnhancedMimeType targetContentType) {
            this.targetContentType = targetContentType;
            return this;
        }

        /**
         * Sets the tenant identifier.
         *
         * @param tenantContext Unique tenant identifier.
         * @return Builder
         */
        public Builder tenantContext(@Nullable final TenantContext tenantContext) {
            this.tenantContext = tenantContext;
            return this;
        }

        /**
         * Sets the maximum time to wait for a single event store call. Without a timeout a call whose
         * future is never completed (for example because the connection is broken) blocks the calling
         * thread forever. Defaults to {@link GrpcCalls#DEFAULT_CALL_TIMEOUT}.
         *
         * @param callTimeout Maximum time to wait for a single call.
         * @return Builder.
         */
        public Builder callTimeout(@Nullable final Duration callTimeout) {
            this.callTimeout = callTimeout;
            return this;
        }

        private void verifyNotNull(final String name, @Nullable final Object value) {
            if (value == null) {
                throw new IllegalStateException(
                        "It is mandatory to set the value of '" + name + "' before calling the 'build()' method");
            }
        }

        /**
         * Creates a new instance of the event store from the attributes set via the
         * builder.
         *
         * @return New event store instance.
         */
        public ESGrpcEventStore build() {
            verifyNotNull("eventStore", eventStore);
            verifyNotNull("serRegistry", serRegistry);
            verifyNotNull("desRegistry", desRegistry);
            verifyNotNull("baseTypeFactory", baseTypeFactory);
            verifyNotNull("targetContentType", targetContentType);
            if (tenantContext == null) {
                tenantContext = new TenantContext.NoopTenantContext();
            }
            final DeserializerRegistry effectiveDesRegistry = converters == null
                    ? desRegistry : new UpcastingDeserializerRegistry(desRegistry, converters);
            return new ESGrpcEventStore(eventStore, serRegistry, effectiveDesRegistry,
                    baseTypeFactory, targetContentType, tenantContext,
                    callTimeout == null ? GrpcCalls.DEFAULT_CALL_TIMEOUT : callTimeout);
        }

    }
}
