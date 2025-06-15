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

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;

import javax.annotation.concurrent.Immutable;

/**
 * Event that is uniquely identified by a UUID. It's equals and hash code methods are defined on the
 * <code>id</code>.
 */
@Immutable
public final class SimpleCommonEvent implements CommonEvent {

    /** The ID of the event, used as part of the idempotent write check. */
    @NotNull
    private EventId id;

    /** The optional unique tenant identifier. */
    @Nullable
    private TenantId tenantId;

    /** Never changing unique event type name. */
    @NotNull
    private TypeName dataType;

    /** The event data. */
    @NotNull
    private Object data;

    /** Never changing unique meta type name. */
    private TypeName metaType;

    /** The meta data. */
    private Object meta;

    /**
     * Protected constructor for deserialization.
     */
    protected SimpleCommonEvent() { //NOSONAR Ignore uninitialized fields
        super();
    }

    /**
     * Constructor without metadata.
     *
     * @param id
     *            The ID of the event, used as part of the idempotent write check. This is type string to
     *            allow different UUID implementations. It has to be a valid UUID string representation.
     * @param dataType
     *            Unique name of the type of data.
     * @param data
     *            Event data.
     * @param tenantId
     *            Optional unique tenant identifier.
     */
    public SimpleCommonEvent(@NotNull final EventId id,
                             @NotNull final TypeName dataType,
                             @NotNull final Object data,
                             @Nullable final TenantId tenantId) {
        this(id, dataType, data, null, null, tenantId);
    }

    /**
     * Constructor with metadata.
     *
     * @param id
     *            The ID of the event, used as part of the idempotent write check. This is type string to
     *            allow different UUID implementations. It has to be a valid UUID string representation.
     * @param dataType
     *            Unique name of the type of data.
     * @param data
     *            Event data.
     * @param metaType
     *            Unique name of the type of metadata.
     * @param meta
     *            Meta data.
     * @param tenantId
     *            Optional unique tenant identifier.
     */
    public SimpleCommonEvent(@NotNull final EventId id,
                             @NotNull final TypeName dataType,
                             @NotNull final Object data,
                             @Nullable final TypeName metaType,
                             @Nullable final Object meta,
                             @Nullable final TenantId tenantId) {//NOSONAR
        super();

        Contract.requireArgNotNull("id", id);
        Contract.requireArgNotNull("type", dataType);
        Contract.requireArgNotNull("data", data);

        this.id = id;
        this.dataType = dataType;
        this.data = data;
        this.metaType = metaType;
        this.meta = meta;
        this.tenantId = tenantId;

    }

    @Override
    public EventId getId() {
        return id;
    }

    @Override
    public TypeName getDataType() {
        return dataType;
    }

    @Nullable
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public TypeName getMetaType() {
        return metaType;
    }

    @Override
    public Object getMeta() {
        return meta;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SimpleCommonEvent other))
            return false;
        if (id == null) {
            return other.id == null;
        } else return id.equals(other.id);
    }


    @Override
    public String toString() {
        return dataType + " " + id;
    }

}
