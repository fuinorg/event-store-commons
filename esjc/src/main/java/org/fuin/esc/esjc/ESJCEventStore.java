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
package org.fuin.esc.esjc;

import static org.fuin.esc.api.ExpectedVersion.ANY;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import jakarta.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ExpectedVersion;
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
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerDeserializerRegistry;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.esc.spi.TenantStreamId;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.EventReadResult;
import com.github.msemys.esjc.EventReadStatus;
import com.github.msemys.esjc.ResolvedEvent;
import com.github.msemys.esjc.SliceReadStatus;
import com.github.msemys.esjc.StreamMetadataResult;
import com.github.msemys.esjc.WriteResult;

/**
 * Implementation that connects to the event store (http://www.geteventstore.com) using the esjc (https://github.com/msemys/esjc) API.
 */
public final class ESJCEventStore extends AbstractReadableEventStore implements IESJCEventStore {

    private final com.github.msemys.esjc.EventStore es;

    private final CommonEvent2EventDataConverter ce2edConv;

    private final RecordedEvent2CommonEventConverter ed2ceConv;

    private final TenantId tenantId;

    private boolean open;

    /**
     * Constructor without a tenant.
     * 
     * @param es
     *            Delegate.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     * @param targetContentType
     *            Target content type (Allows only 'application/xml' or 'application/json' with 'utf-8' encoding).
     * 
     * @deprecated Use the Builder in this class instead.
     */
    @Deprecated
    public ESJCEventStore(@NotNull final com.github.msemys.esjc.EventStore es, @NotNull final SerializerRegistry serRegistry,
            @NotNull final DeserializerRegistry desRegistry, @NotNull final EnhancedMimeType targetContentType) {
        this(es, serRegistry, desRegistry, targetContentType, null);
    }

    /**
     * Private constructor with all data used by the builder.
     * 
     * @param es
     *            Delegate.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     * @param targetContentType
     *            Target content type (Allows only 'application/xml' or 'application/json' with 'utf-8' encoding).
     * @param tenantId
     *            Unique tenant identifier.
     */
    private ESJCEventStore(@NotNull final com.github.msemys.esjc.EventStore es, @NotNull final SerializerRegistry serRegistry,
            @NotNull final DeserializerRegistry desRegistry, @NotNull final EnhancedMimeType targetContentType,
            @Nullable final TenantId tenantId) {
        super();
        Contract.requireArgNotNull("es", es);
        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("desRegistry", desRegistry);
        Contract.requireArgNotNull("targetContentType", targetContentType);
        this.es = es;
        this.ce2edConv = new CommonEvent2EventDataConverter(serRegistry, targetContentType);
        this.ed2ceConv = new RecordedEvent2CommonEventConverter(desRegistry);
        this.tenantId = tenantId;
        this.open = false;
    }

    @Override
    public final ESJCEventStore open() {
        if (open) {
            // Ignore
            return this;
        }
        es.connect();
        this.open = true;
        return this;
    }

    @Override
    public final void close() {
        if (!open) {
            // Ignore
            return;
        }
        es.disconnect();
        this.open = false;
    }

    @Override
    public final boolean isSupportsCreateStream() {
        return false;
    }

    @Override
    public final void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Do nothing as the operation is not supported
    }

    @Override
    public final long appendToStream(final StreamId streamId, final CommonEvent... events)
            throws StreamNotFoundException, StreamDeletedException, StreamReadOnlyException {
        return appendToStream(streamId, -2, EscSpiUtils.asList(events));
    }

    @Override
    public final long appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events)
            throws StreamNotFoundException, StreamDeletedException, WrongExpectedVersionException, StreamReadOnlyException {
        return appendToStream(streamId, expectedVersion, EscSpiUtils.asList(events));
    }

    @Override
    public long appendToStream(final StreamId streamId, final List<CommonEvent> events)
            throws StreamNotFoundException, StreamDeletedException, StreamReadOnlyException {
        return appendToStream(streamId, -2, events);
    }

    @Override
    public final long appendToStream(final StreamId streamId, final long expectedVersion, final List<CommonEvent> commonEvents)
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
            final Iterable<EventData> eventDataIt = asEventData(commonEvents);
            final WriteResult result = es.appendToStream(sid.asString(), expectedVersion, eventDataIt).get();
            return result.nextExpectedVersion;
        } catch (final ExecutionException ex) {
            if (ex.getCause() instanceof com.github.msemys.esjc.operation.WrongExpectedVersionException) {
                com.github.msemys.esjc.operation.WrongExpectedVersionException cause = (com.github.msemys.esjc.operation.WrongExpectedVersionException) ex
                        .getCause();
                throw new WrongExpectedVersionException(sid, expectedVersion, cause.currentVersion);
            }
            if (ex.getCause() instanceof com.github.msemys.esjc.operation.StreamDeletedException) {
                throw new StreamDeletedException(sid);
            }
            throw new RuntimeException("Error executing append", ex);
        } catch (InterruptedException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for append result", ex);
        }

    }

    @Override
    public final void deleteStream(final StreamId streamId, final long expectedVersion, final boolean hardDelete)
            throws StreamDeletedException, WrongExpectedVersionException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);

        if (sid.isProjection()) {
            throw new StreamReadOnlyException(sid);
        }

        try {
            es.deleteStream(sid.asString(), expectedVersion, hardDelete).get();
        } catch (final ExecutionException ex) {
            if (ex.getCause() instanceof com.github.msemys.esjc.operation.WrongExpectedVersionException) {
                com.github.msemys.esjc.operation.WrongExpectedVersionException cause = (com.github.msemys.esjc.operation.WrongExpectedVersionException) ex
                        .getCause();
                throw new WrongExpectedVersionException(sid, expectedVersion, cause.currentVersion);
            }
            if (ex.getCause() instanceof com.github.msemys.esjc.operation.StreamDeletedException) {
                throw new StreamDeletedException(sid);
            }
            throw new RuntimeException("Error executing delete", ex);
        } catch (final InterruptedException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for delete result", ex);
        }

    }

    @Override
    public final void deleteStream(final StreamId streamId, final boolean hardDelete)
            throws StreamNotFoundException, StreamDeletedException {

        deleteStream(streamId, ANY.getNo(), hardDelete);

    }

    @Override
    public final StreamEventsSlice readEventsForward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        try {
            final com.github.msemys.esjc.StreamEventsSlice slice = es.readStreamEventsForward(sid.asString(), start, count, true).get();
            if (SliceReadStatus.StreamDeleted == slice.status) {
                throw new StreamDeletedException(sid);
            }
            if (SliceReadStatus.StreamNotFound == slice.status) {
                throw new StreamNotFoundException(sid);
            }
            final List<CommonEvent> events = asCommonEvents(slice.events);
            final boolean endOfStream = count > events.size();
            return new StreamEventsSlice(slice.fromEventNumber, events, slice.nextEventNumber, endOfStream);
        } catch (InterruptedException | ExecutionException ex) {// NOSONAR
            throw new RuntimeException("Error waiting for read forward result", ex);
        }

    }

    @Override
    public final StreamEventsSlice readEventsBackward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        try {
            final com.github.msemys.esjc.StreamEventsSlice slice = es.readStreamEventsBackward(sid.asString(), start, count, true).get();
            if (SliceReadStatus.StreamDeleted == slice.status) {
                throw new StreamDeletedException(sid);
            }
            if (SliceReadStatus.StreamNotFound == slice.status) {
                throw new StreamNotFoundException(sid);
            }
            final List<CommonEvent> events = asCommonEvents(slice.events);
            long nextEventNumber = slice.nextEventNumber;
            final boolean endOfStream = (start - count < 0);
            if (endOfStream) {
                nextEventNumber = 0;
            }
            return new StreamEventsSlice(slice.fromEventNumber, events, nextEventNumber, endOfStream);
        } catch (InterruptedException | ExecutionException ex) {// NOSONAR
            throw new RuntimeException("Error waiting for read forward result", ex);
        }

    }

    @Override
    public final CommonEvent readEvent(final StreamId streamId, final long eventNumber) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        try {
            final EventReadResult eventReadResult = es.readEvent(sid.asString(), eventNumber, true).get();
            if (eventReadResult.status == EventReadStatus.NoStream) {
                throw new StreamNotFoundException(sid);
            }
            if (eventReadResult.status == EventReadStatus.NotFound) {
                throw new EventNotFoundException(sid, eventNumber);
            }
            if (eventReadResult.status == EventReadStatus.StreamDeleted) {
                throw new StreamDeletedException(sid);
            }
            return asCommonEvent(eventReadResult.event);
        } catch (InterruptedException | ExecutionException ex) {// NOSONAR
            throw new RuntimeException("Error waiting for read forward result", ex);
        }

    }

    @Override
    public final boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        try {
            final com.github.msemys.esjc.StreamEventsSlice slice = es.readStreamEventsForward(sid.asString(), 0, 1, false).get();
            if (SliceReadStatus.StreamDeleted == slice.status) {
                return false;
            }
            if (SliceReadStatus.StreamNotFound == slice.status) {
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException ex) {// NOSONAR
            throw new RuntimeException("Error waiting for read forward result", ex);
        }

    }

    @Override
    public final StreamState streamState(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        try {

            final com.github.msemys.esjc.StreamEventsSlice slice = es.readStreamEventsForward(sid.asString(), 0, 1, false).get();
            if (SliceReadStatus.StreamDeleted == slice.status) {
                final StreamMetadataResult result = es.getStreamMetadata(sid.asString()).get();
                if (result.isStreamDeleted) {
                    return StreamState.HARD_DELETED;
                }
                return StreamState.SOFT_DELETED;
            }
            if (SliceReadStatus.StreamNotFound == slice.status) {
                throw new StreamNotFoundException(sid);
            }
            return StreamState.ACTIVE;

        } catch (InterruptedException | ExecutionException ex) {// NOSONAR
            throw new RuntimeException("Error waiting for read forward result", ex);
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
        return ed2ceConv.convert(resolvedEvent.event);
    }

    private void ensureOpen() {
        if (!open) {
            open();
        }
    }

    /**
     * Builder used to create a new instance of the event store.
     */
    public static final class Builder {

        private com.github.msemys.esjc.EventStore eventStore;

        private SerializerRegistry serRegistry;

        private DeserializerRegistry desRegistry;

        private EnhancedMimeType targetContentType;

        private TenantId tenantId;

        /**
         * Sets the event store to use internally.
         * 
         * @param eventStore
         *            TCP/IP event store connection.
         * 
         * @return Builder.
         */
        public Builder eventStore(com.github.msemys.esjc.EventStore eventStore) {
            this.eventStore = eventStore;
            return this;
        }

        /**
         * Sets the serializer registry.
         * 
         * @param serRegistry
         *            Registry used to locate serializers.
         * 
         * @return Builder.
         */
        public Builder serRegistry(final SerializerRegistry serRegistry) {
            this.serRegistry = serRegistry;
            return this;
        }

        /**
         * Sets the deserializer registry.
         * 
         * @param desRegistry
         *            Registry used to locate deserializers.
         * 
         * @return Builder.
         */
        public Builder desRegistry(final DeserializerRegistry desRegistry) {
            this.desRegistry = desRegistry;
            return this;
        }

        /**
         * Sets both types of registries in one call.
         * 
         * @param registry
         *            Serializer/Deserializer registry to set.
         * 
         * @return Builder.
         */
        public Builder serDesRegistry(final SerDeserializerRegistry registry) {
            this.serRegistry = registry;
            this.desRegistry = registry;
            return this;
        }

        /**
         * Sets the target content type.
         * 
         * @param targetContentType
         *            Target content type (Allows only 'application/xml' or 'application/json' with 'utf-8' encoding).
         * 
         * @return Builder.
         */
        public Builder targetContentType(final EnhancedMimeType targetContentType) {
            this.targetContentType = targetContentType;
            return this;
        }

        /**
         * Sets the tenant identifier.
         * 
         * @param tenantId
         *            Unique tenant identifier.
         * 
         * @return Builder
         */
        public Builder tenantId(final TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        private void verifyNotNull(final String name, final Object value) {
            if (value == null) {
                throw new IllegalStateException("It is mandatory to set the value of '" + name + "' before calling the 'build()' method");
            }
        }

        /**
         * Creates a new instance of the event store from the attributes set via the builder.
         * 
         * @return New event store instance.
         */
        public ESJCEventStore build() {
            verifyNotNull("eventStore", eventStore);
            verifyNotNull("serRegistry", serRegistry);
            verifyNotNull("desRegistry", desRegistry);
            verifyNotNull("targetContentType", targetContentType);
            return new ESJCEventStore(eventStore, serRegistry, desRegistry, targetContentType, tenantId);
        }

    }
}
