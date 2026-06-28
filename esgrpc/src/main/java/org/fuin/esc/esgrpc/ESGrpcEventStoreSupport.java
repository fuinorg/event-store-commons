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

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.kurrent.dbclient.EventData;
import io.kurrent.dbclient.RecordedEvent;
import io.kurrent.dbclient.ResolvedEvent;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.IBaseTypeFactory;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.TenantContext;
import org.fuin.esc.api.TenantStreamId;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.fuin.esc.api.ExpectedVersion.ANY;

/**
 * Bundles the shared state (gRPC client, converters and tenant context) and the helper logic that is
 * used by both the synchronous {@link ESGrpcEventStore} and the asynchronous {@link ESGrpcEventStoreAsync}.
 * Extracting it here keeps the event/exception conversion in a single place instead of being duplicated
 * across the two stores.
 */
@ThreadSafe
final class ESGrpcEventStoreSupport {

    private final CommonEvent2EventDataConverter ce2edConv;

    private final RecordedEvent2CommonEventConverter ed2ceConv;

    private final TenantContext tenantContext;

    /**
     * Constructor with all mandatory data.
     *
     * @param serRegistry       Registry used to locate serializers.
     * @param desRegistry       Registry used to locate deserializers.
     * @param baseTypeFactory   Factory used to create basic types.
     * @param targetContentType Target content type (Allows only 'application/xml'
     *                          or 'application/json' with 'utf-8' encoding).
     * @param tenantContext     Provides the current tenant.
     */
    ESGrpcEventStoreSupport(final SerializerRegistry serRegistry,
                            final DeserializerRegistry desRegistry,
                            final IBaseTypeFactory baseTypeFactory,
                            final EnhancedMimeType targetContentType,
                            final TenantContext tenantContext) {
        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("desRegistry", desRegistry);
        Contract.requireArgNotNull("baseTypeFactory", baseTypeFactory);
        Contract.requireArgNotNull("targetContentType", targetContentType);
        Contract.requireArgNotNull("tenantContext", tenantContext);
        this.ce2edConv = new CommonEvent2EventDataConverter(serRegistry, baseTypeFactory, targetContentType);
        this.ed2ceConv = new RecordedEvent2CommonEventConverter(desRegistry);
        this.tenantContext = tenantContext;
    }

    /**
     * Returns the tenant aware stream identifier for a given stream.
     *
     * @param streamId Stream to wrap.
     * @return Tenant aware stream identifier.
     */
    TenantStreamId sid(final StreamId streamId) {
        return new TenantStreamId(tenantContext.getTenantId().orElse(null), streamId);
    }

    /**
     * Converts a list of common events into a list of gRPC event data.
     *
     * @param commonEvents Events to convert.
     * @return Converted events.
     */
    List<EventData> asEventData(final List<CommonEvent> commonEvents) {
        final List<EventData> list = new ArrayList<>(commonEvents.size());
        for (final CommonEvent commonEvent : commonEvents) {
            list.add(ce2edConv.convert(commonEvent));
        }
        return list;
    }

    /**
     * Converts a list of resolved gRPC events into a list of common events.
     *
     * @param resolvedEvents Events to convert.
     * @return Converted events.
     */
    List<CommonEvent> asCommonEvents(final List<ResolvedEvent> resolvedEvents) {
        final List<CommonEvent> list = new ArrayList<>(resolvedEvents.size());
        for (final ResolvedEvent resolvedEvent : resolvedEvents) {
            list.add(asCommonEvent(resolvedEvent.getEvent()));
        }
        return list;
    }

    /**
     * Converts a single recorded gRPC event into a common event.
     *
     * @param recordedEvent Event to convert.
     * @return Converted event.
     */
    CommonEvent asCommonEvent(final RecordedEvent recordedEvent) {
        return ed2ceConv.convert(recordedEvent);
    }

    /**
     * Returns the name of the metadata stream that backs a given stream. Used as a workaround to
     * distinguish a soft deleted from a never existing stream.
     *
     * @param streamId Stream to determine the metadata stream name for.
     * @return Name of the metadata stream.
     */
    static String metaStreamName(final StreamId streamId) {
        return "$$" + streamId.asString();
    }

    /**
     * Verifies that a given stream is not a projection (which would be read only).
     *
     * @param streamId Stream to verify.
     * @throws StreamReadOnlyException The given stream identifier points to a projection.
     */
    static void ensureStreamNoProjection(final StreamId streamId) {
        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }
    }

    /**
     * Determines if a given (already unwrapped) failure cause signals a deleted stream.
     *
     * @param cause Cause to inspect (may be {@code null}).
     * @return {@code true} if the cause indicates a deleted stream.
     */
    static boolean statusIsDeleted(@Nullable final Throwable cause) {
        if (cause instanceof StatusRuntimeException sre) {
            return sre.getStatus().getCode().equals(Status.FAILED_PRECONDITION.getCode())
                    && sre.getStatus().getDescription() != null
                    && sre.getStatus().getDescription().contains("is deleted");
        }
        return cause instanceof io.kurrent.dbclient.StreamDeletedException;
    }

    /**
     * Maps an expected event store version to the gRPC stream state.
     *
     * @param version Expected version.
     * @return Corresponding gRPC stream state.
     */
    static io.kurrent.dbclient.StreamState version2State(final long version) {
        if (version == ANY.getNo()) {
            return io.kurrent.dbclient.StreamState.any();
        }
        if (version == ExpectedVersion.NO_OR_EMPTY_STREAM.getNo()) {
            return io.kurrent.dbclient.StreamState.noStream();
        }
        return io.kurrent.dbclient.StreamState.streamRevision(version);
    }

    /**
     * Translates a gRPC client failure into the matching event-store-commons exception. The returned
     * exception is meant to be thrown (synchronous) or to complete a future exceptionally (asynchronous).
     *
     * @param cause           Already unwrapped failure cause (may be {@code null}).
     * @param sid             Stream the operation was executed on.
     * @param expectedVersion Expected version used by the operation (pass {@link ExpectedVersion#ANY} for reads).
     * @return Mapped exception.
     */
    static RuntimeException mapException(@Nullable final Throwable cause, final TenantStreamId sid,
                                         final long expectedVersion) {
        if (cause instanceof io.kurrent.dbclient.WrongExpectedVersionException wevex) {
            return new WrongExpectedVersionException(sid, expectedVersion, wevex.getActualState().toRawLong());
        }
        if (statusIsDeleted(cause)) {
            return new StreamDeletedException(sid);
        }
        if (cause instanceof io.kurrent.dbclient.StreamNotFoundException) {
            return new StreamNotFoundException(sid);
        }
        return new RuntimeException("Error executing event store operation", cause);
    }

}
