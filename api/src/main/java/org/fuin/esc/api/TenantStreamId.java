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
package org.fuin.esc.api;

import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.core.KeyValue;

import java.io.Serial;
import java.util.List;

/**
 * Represents a stream identifier that may belong to a tenant.
 * In case the tenant ID is {@literal null}, it's a normal stream.
 */
public final class TenantStreamId implements StreamId {

    /**
     * Prefix used for projection streams that are emitted by projections.
     * Will be used if the stream ID is of type {@link ProjectionStreamId}.
     */
    public static final String PROJECTION_PREFIX = "projection-";

    @Serial
    private static final long serialVersionUID = 1000L;

    @Nullable
    private final TenantId tenantId;

    private final StreamId streamId;

    public TenantStreamId(@Nullable final TenantId tenantId, final StreamId streamId) {
        super();
        Contract.requireArgNotNull("streamId", streamId);
        this.tenantId = tenantId;
        this.streamId = streamId;
    }

    @Override
    @NotNull
    public String getName() {
        if (tenantId == null) {
            if (streamId instanceof ProjectionStreamId) {
                return PROJECTION_PREFIX + streamId.asString();
            }
            return streamId.asString();
        }
        if (streamId instanceof ProjectionStreamId) {
            return PROJECTION_PREFIX + tenantId + "-" + streamId.asString();
        }
        return tenantId + "-" + streamId.asString();
    }

    @Override
    public boolean isProjection() {
        return streamId.isProjection();
    }

    @Override
    @NotNull
    public <T> T getSingleParamValue() {
        return streamId.getSingleParamValue();
    }

    @Override
    @NotNull
    public List<KeyValue> getParameters() {
        return streamId.getParameters();
    }

    @Override
    @NotNull
    public String asString() {
        return getName();
    }

    /**
     * Returns the tenant identifier.
     *
     * @return Tenant identifier.
     */
    @Nullable
    public TenantId getTenantId() {
        return tenantId;
    }

    /**
     * Returns the underlying stream identifier.
     *
     * @return Stream identifier.
     */
    @NotNull
    public StreamId getStreamId() {
        return streamId;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TenantStreamId other = (TenantStreamId) obj;
        return getName().equals(other.getName());
    }

    @Override
    public String toString() {
        return getName();
    }

}
