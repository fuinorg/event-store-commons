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
package org.fuin.esc.spi;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TenantId;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;
import org.fuin.objects4j.vo.KeyValue;

/**
 * Represents a tenant stream identifier.
 */
public final class TenantStreamId implements StreamId {

    private static final long serialVersionUID = 1000L;

    private final TenantId tenantId;

    private final StreamId delegate;

    public TenantStreamId(@Nullable final TenantId tenantId, @NotNull final StreamId streamId) {
        super();
        Contract.requireArgNotNull("streamId", streamId);
        this.tenantId = tenantId;
        this.delegate = streamId;
    }

    @Override
    @NotNull
    public String getName() {
        if (tenantId == null) {
            return delegate.getName();
        }
        return tenantId + "-" + delegate.getName();
    }

    @Override
    public boolean isProjection() {
        return delegate.isProjection();
    }

    @Override
    @NotNull
    public <T> T getSingleParamValue() {
        return delegate.getSingleParamValue();
    }

    @Override
    @NotNull
    public List<KeyValue> getParameters() {
        return delegate.getParameters();
    }

    @Override
    @NotNull
    public String asString() {
        if (tenantId == null) {
            return delegate.asString();
        }
        return tenantId.asString() + "-" + delegate.asString();
    }

    /**
     * Returns the tenant identifier.
     * 
     * @return Tenant identifier.
     */
    public TenantId getTenantId() {
        return tenantId;
    }

    /**
     * Returns the underlying stream identifier.
     * 
     * @return Stream identifier.
     */
    public StreamId getDelegate() {
        return delegate;
    }

    @Override
    public final int hashCode() {
        return asString().hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
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
        return asString().equals(other.asString());
    }

    @Override
    public final String toString() {
        return asString();
    }

}
