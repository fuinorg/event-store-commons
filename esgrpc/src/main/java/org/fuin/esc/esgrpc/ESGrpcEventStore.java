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

import io.kurrent.dbclient.AppendToStreamOptions;
import io.kurrent.dbclient.DeleteStreamOptions;
import io.kurrent.dbclient.EventData;
import io.kurrent.dbclient.KurrentDBClient;
import io.kurrent.dbclient.ReadResult;
import io.kurrent.dbclient.ReadStreamOptions;
import io.kurrent.dbclient.ResolvedEvent;
import io.kurrent.dbclient.WriteResult;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.IBaseTypeFactory;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.esc.spi.AbstractReadableEventStore;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.TenantStreamId;
import org.fuin.objects4j.common.Contract;
import org.fuin.utils4j.TestOmitted;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.fuin.esc.api.ExpectedVersion.ANY;

/**
 * Implementation that connects to the event store (<a href="http://www.geteventstore.com">Eventstore</a>) using the GRPC API.
 */
@TestOmitted("Tested in the 'test' project")
public final class ESGrpcEventStore extends AbstractReadableEventStore implements IESGrpcEventStore {

    private final KurrentDBClient es;

    private final CommonEvent2EventDataConverter ce2edConv;

    private final RecordedEvent2CommonEventConverter ed2ceConv;

    private final TenantId tenantId;

    /**
     * Private constructor with all data used by the builder.
     *
     * @param es                Delegate.
     * @param serRegistry       Registry used to locate serializers.
     * @param desRegistry       Registry used to locate deserializers.
     * @param baseTypeFactory   Factory used to create basic types.
     * @param targetContentType Target content type (Allows only 'application/xml'
     *                          or 'application/json' with 'utf-8' encoding).
     * @param tenantId          Unique tenant identifier.
     */
    private ESGrpcEventStore(@NotNull final KurrentDBClient es,
                             @NotNull final SerializerRegistry serRegistry,
                             @NotNull final DeserializerRegistry desRegistry,
                             @NotNull final IBaseTypeFactory baseTypeFactory,
                             @NotNull final EnhancedMimeType targetContentType,
                             @Nullable final TenantId tenantId) {
        super();
        Contract.requireArgNotNull("es", es);
        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("desRegistry", desRegistry);
        Contract.requireArgNotNull("baseTypeFactory", baseTypeFactory);
        Contract.requireArgNotNull("targetContentType", targetContentType);
        this.es = es;
        this.ce2edConv = new CommonEvent2EventDataConverter(serRegistry, baseTypeFactory, targetContentType);
        this.ed2ceConv = new RecordedEvent2CommonEventConverter(desRegistry);
        this.tenantId = tenantId;
    }

    @Override
    public ESGrpcEventStore open() {
        // Do nothing - We assume that the eventstore is already
        // fully initialized when passed in to constructor
        return this;
    }

    @Override
    public void close() {
        if (!es.isShutdown()) {
            es.shutdown();
        }
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
        return appendToStream(streamId, -2, EscSpiUtils.asList(events));
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events)
            throws StreamNotFoundException, StreamDeletedException, WrongExpectedVersionException,
            StreamReadOnlyException {
        return appendToStream(streamId, expectedVersion, EscSpiUtils.asList(events));
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
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);

        if (sid.isProjection()) {
            throw new StreamReadOnlyException(sid);
        }

        try {
            final Iterator<EventData> eventDataIt = asEventData(commonEvents).iterator();
            final WriteResult result = es.appendToStream(sid.asString(),
                    AppendToStreamOptions.get().streamState(version2State(expectedVersion)), eventDataIt).get();
            return result.getNextExpectedRevision().toRawLong();
        } catch (final ExecutionException ex) {
            if (ex.getCause() instanceof io.kurrent.dbclient.WrongExpectedVersionException cause) {
                throw new WrongExpectedVersionException(sid, expectedVersion, cause.getActualState().toRawLong());
            }
            if (statusIsDeleted(ex)) {
                throw new StreamDeletedException(sid);
            }
            if (ex.getCause() instanceof io.kurrent.dbclient.StreamNotFoundException) {
                throw new StreamNotFoundException(sid);
            }
            throw new RuntimeException("Error executing appendToStream(..)", ex);
        } catch (InterruptedException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for appendToStream(..) result", ex);
        }

    }

    @Override
    public void deleteStream(final StreamId streamId, final long expectedVersion, final boolean hardDelete)
            throws StreamDeletedException, WrongExpectedVersionException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);

        if (sid.isProjection()) {
            throw new StreamReadOnlyException(sid);
        }

        try {
            final DeleteStreamOptions options = DeleteStreamOptions.get()
                    .streamState(version2State(expectedVersion));
            if (hardDelete) {
                es.tombstoneStream(sid.asString(), options).get();
            } else {
                es.deleteStream(sid.asString(), options).get();
            }
        } catch (final ExecutionException ex) {
            if (ex.getCause() instanceof io.kurrent.dbclient.WrongExpectedVersionException cause) {
                throw new WrongExpectedVersionException(sid, expectedVersion, cause.getActualState().toRawLong());
            }
            if (statusIsDeleted(ex)) {
                throw new StreamDeletedException(sid);
            }
            if (ex.getCause() instanceof io.kurrent.dbclient.StreamNotFoundException) {
                throw new StreamNotFoundException(sid);
            }
            throw new RuntimeException("Error executing deleteStream(..)", ex);
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

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        try {

            final ReadStreamOptions options = ReadStreamOptions.get().forwards().fromRevision(start).maxCount(count)
                    .resolveLinkTos();

            final ReadResult readResult = es.readStream(sid.asString(), options).get();
            final List<CommonEvent> events = asCommonEvents(readResult.getEvents());
            final boolean endOfStream = count > events.size();
            return new StreamEventsSlice(start, events, start + events.size(), endOfStream);
        } catch (ExecutionException ex) {
            if (statusIsDeleted(ex)) {
                throw new StreamDeletedException(sid);
            }
            if (ex.getCause() instanceof io.kurrent.dbclient.StreamNotFoundException) {
                throw new StreamNotFoundException(sid);
            }
            throw new RuntimeException("Error executing readEventsForward(..)", ex);
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

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        try {
            final ReadStreamOptions options = ReadStreamOptions.get().backwards().fromRevision(start).maxCount(count)
                    .resolveLinkTos();
            final ReadResult slice = es.readStream(sid.asString(), options).get();
            final List<CommonEvent> events = asCommonEvents(slice.getEvents());
            long nextEventNumber = start - events.size();
            final boolean endOfStream = (start - count < 0);
            if (endOfStream) {
                nextEventNumber = 0;
            }
            return new StreamEventsSlice(start, events, nextEventNumber, endOfStream);
        } catch (ExecutionException ex) {
            if (statusIsDeleted(ex)) {
                throw new StreamDeletedException(sid);
            }
            if (ex.getCause() instanceof io.kurrent.dbclient.StreamNotFoundException) {
                throw new StreamNotFoundException(sid);
            }
            throw new RuntimeException("Error executing readEventsBackward(..)", ex);
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

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        try {
            final ReadStreamOptions options = ReadStreamOptions.get().forwards().fromRevision(0).maxCount(1);
            es.readStream(sid.asString(), options).get();
            return true;
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof StatusRuntimeException) {
                return false;
            }
            if (ex.getCause() instanceof io.kurrent.dbclient.StreamNotFoundException) {
                return false;
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

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        try {
            es.readStream(sid.asString(), ReadStreamOptions.get().forwards().fromRevision(0)).get();
            return StreamState.ACTIVE;
        } catch (ExecutionException ex) {
            if (statusIsDeleted(ex)) {
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
            es.readStream("$$" + streamId.asString(), ReadStreamOptions.get().forwards().fromRevision(0)).get();
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

    private List<EventData> asEventData(final List<CommonEvent> commonEvents) {
        final List<EventData> list = new ArrayList<>(commonEvents.size());
        for (final CommonEvent commonEvent : commonEvents) {
            list.add(ce2edConv.convert(commonEvent));
        }
        return list;
    }

    private List<CommonEvent> asCommonEvents(final List<ResolvedEvent> resolvedEvents) {
        final List<CommonEvent> list = new ArrayList<>(resolvedEvents.size());
        for (final ResolvedEvent resolvedEvent : resolvedEvents) {
            list.add(asCommonEvent(resolvedEvent));
        }
        return list;
    }

    private CommonEvent asCommonEvent(final ResolvedEvent resolvedEvent) {
        return ed2ceConv.convert(resolvedEvent.getEvent());
    }

    private void ensureOpen() {
        if (es.isShutdown()) {
            throw new IllegalStateException("The event store has already been closed");
        }
    }

    private static boolean statusIsDeleted(ExecutionException ex) {
        if (ex.getCause() instanceof StatusRuntimeException sre) {
            return sre.getStatus().getCode().equals(Status.FAILED_PRECONDITION.getCode())
                    && sre.getStatus().getDescription() != null
                    && sre.getStatus().getDescription().contains("is deleted");
        }
        return ex.getCause() instanceof io.kurrent.dbclient.StreamDeletedException;
    }

    private static io.kurrent.dbclient.StreamState version2State(long version) {
        if (version == ANY.getNo()) {
            return io.kurrent.dbclient.StreamState.any();
        }
        if (version == ExpectedVersion.NO_OR_EMPTY_STREAM.getNo()) {
            return io.kurrent.dbclient.StreamState.noStream();
        }
        return io.kurrent.dbclient.StreamState.streamRevision(version);
    }

    /**
     * Builder used to create a new instance of the event store.
     */
    public static final class Builder {

        private io.kurrent.dbclient.KurrentDBClient eventStore;

        private SerializerRegistry serRegistry;

        private DeserializerRegistry desRegistry;

        private IBaseTypeFactory baseTypeFactory;

        private EnhancedMimeType targetContentType;

        private TenantId tenantId;

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
         * @param tenantId Unique tenant identifier.
         * @return Builder
         */
        public Builder tenantId(final TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        private void verifyNotNull(final String name, final Object value) {
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
            return new ESGrpcEventStore(eventStore, serRegistry, desRegistry, baseTypeFactory, targetContentType, tenantId);
        }

    }
}
