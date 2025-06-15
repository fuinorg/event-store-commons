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

/**
 * Event that is uniquely identified by a UUID. It's equals and hash code methods are defined on the
 * <code>id</code>.
 */
public interface CommonEvent {

    /**
     * Returns the ID of the event, used as part of the idempotent write check. This is type string to allow
     * different UUID implementations. It has to be a valid UUID string representation.
     *
     * @return Unique event identifier.
     */
    @NotNull
    EventId getId();

    /**
     * Returns the event type.
     *
     * @return Never changing unique event type name.
     */
    @NotNull
    TypeName getDataType();

    /**
     * Returns the unique tenant identifier.
     *
     * @return Optional tenant ID.
     */
    @Nullable
    TenantId getTenantId();

    /**
     * Returns the event data.
     *
     * @return Event data.
     */
    @NotNull
    Object getData();

    /**
     * Returns the metadata type.
     *
     * @return Never changing unique event meta data type name.
     */
    @Nullable
    TypeName getMetaType();

    /**
     * Returns the metadata.
     *
     * @return Meta data.
     */
    @Nullable
    Object getMeta();

}
