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
package org.fuin.esc.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.Contract;

/**
 * Database table for events of streams that do not have any parameters.
 */
@Table(name = NoParamsEvent.NO_PARAMS_EVENTS_TABLE)
@Entity
@IdClass(NoParamsEventPrimaryKey.class)
public class NoParamsEvent extends JpaStreamEvent {

    /** Name of the table. */
    public static final String NO_PARAMS_EVENTS_TABLE = "no_params_events";
    
    @Id
    @NotNull
    @Column(name = "STREAM_NAME", nullable = false, updatable = false, length = 100)
    private String streamName;

    @Id
    @NotNull
    @Column(name = "EVENT_NUMBER")
    private Integer eventNumber;

    /**
     * Protected default constructor only required for JPA.
     */
    protected NoParamsEvent() {
        super();
    }

    /**
     * Constructor with all mandatory data.
     * 
     * @param streamId
     *            Unique identifier of the stream.
     * @param version
     *            Version.
     * @param eventEntry
     *            Event entry to connect.
     */
    public NoParamsEvent(@NotNull final StreamId streamId, @NotNull final Integer version,
            @NotNull final JpaEvent eventEntry) {
        super(eventEntry);
        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("version", version);
        this.streamName = streamId.getName();
        this.eventNumber = version;
    }

    /**
     * Returns the number of the stream.
     * 
     * @return Number that is unique in combination with the name.
     */
    public final Integer getEventNumber() {
        return eventNumber;
    }

    // CHECKSTYLE:OFF Generated code

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((streamName == null) ? 0 : streamName.hashCode());
        result = prime * result + ((eventNumber == null) ? 0 : eventNumber.hashCode());
        return result;
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
        NoParamsEvent other = (NoParamsEvent) obj;
        if (streamName == null) {
            if (other.streamName != null) {
                return false;
            }
        } else if (!streamName.equals(other.streamName)) {
            return false;
        }
        if (eventNumber == null) {
            if (other.eventNumber != null) {
                return false;
            }
        } else if (!eventNumber.equals(other.eventNumber)) {
            return false;
        }
        return true;
    }

    // CHECKSTYLE:ON

    @Override
    public final String toString() {
        return streamName + " " + eventNumber;
    }

}
