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
package org.fuin.esc.api;

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Nullable;

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
    public EventId getId();

    /**
     * Returns the event type.
     * 
     * @return Never changing unique event type name.
     */
    @NotNull
    public EventType getType();

    /**
     * Returns the event data.
     * 
     * @return Event data.
     */
    @NotNull
    public Object getData();

    /**
     * Returns the meta data.
     * 
     * @return Meta data.
     */
    @Nullable
    public Object getMeta();

}
