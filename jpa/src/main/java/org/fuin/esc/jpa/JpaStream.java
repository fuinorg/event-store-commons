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

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamState;

/**
 * Base class for all concrete streams.
 */
@MappedSuperclass
public abstract class JpaStream {

    @Column(name = "STATE", nullable = false)
    private int state = StreamState.ACTIVE.dbValue();

    @Column(name = "VERSION", nullable = false)
    private long version = -1;

    /**
     * Returns the state of the stream.
     *
     * @return State.
     */
    public StreamState getState() {
        return StreamState.fromDbValue(state);
    }

    /**
     * Returns the information if the stream was deleted.
     *
     * @return TRUE if soft or hard deleted.
     */
    public boolean isDeleted() {
        return (state == StreamState.SOFT_DELETED.dbValue() || state == StreamState.HARD_DELETED.dbValue());
    }

    /**
     * Marks the stream as deleted.
     *
     * @param hardDelete
     *            Hard or soft deletion.
     */
    public void delete(final boolean hardDelete) {
        if (hardDelete) {
            this.state = StreamState.HARD_DELETED.dbValue();
        } else {
            this.state = StreamState.SOFT_DELETED.dbValue();
        }
    }

    /**
     * Returns the current version of the stream.
     *
     * @return Version.
     */
    public long getVersion() {
        return version;
    }

    /**
     * Increments the version of the stream by one.
     *
     * @return New version.
     */
    public long incVersion() {
        return ++version;
    }

    /**
     * Creates a container that stores the given event entry.
     *
     * @param streamId
     *            The unique identifier of the stream to create an event for.
     * @param eventEntry
     *            Event entry to convert into a JPA variant.
     *
     * @return JPA entity.
     */
    public abstract JpaStreamEvent createEvent(@NotNull StreamId streamId, @NotNull JpaEvent eventEntry);

}
