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
package org.fuin.esc.jpa;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.Contract;

/**
 * Identifies a stream event based on a stream name and an event number.
 */
public final class NoParamsEventPrimaryKey implements Serializable {

    private static final long serialVersionUID = 1000L;

    private String streamName;

    private Long eventNumber;

    /**
     * Default constructor for JPA. <b><i>CAUTION:</i> DO NOT USE IN APPLICATION CODE.</b>
     */
    public NoParamsEventPrimaryKey() {
        super();
    }

    /**
     * Constructor with all required data.
     * 
     * @param streamId
     *            Unique stream identifier.
     * @param eventNumber
     *            Number of the event within the stream.
     */
    public NoParamsEventPrimaryKey(@NotNull final StreamId streamId, @NotNull final Long eventNumber) {
        super();
        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("eventNumber", eventNumber);
        this.streamName = streamId.getName();
        this.eventNumber = eventNumber;
    }

    /**
     * Returns the name of the stream.
     * 
     * @return Unique stream identifier name.
     */
    @NotNull
    public final String getStreamName() {
        return streamName;
    }

    /**
     * Returns the number of the event within the stream.
     * 
     * @return Order of the event in the stream.
     */
    @NotNull
    public final Long getEventNumber() {
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NoParamsEventPrimaryKey other = (NoParamsEventPrimaryKey) obj;
        if (streamName == null) {
            if (other.streamName != null)
                return false;
        } else if (!streamName.equals(other.streamName))
            return false;
        if (eventNumber == null) {
            if (other.eventNumber != null)
                return false;
        } else if (!eventNumber.equals(other.eventNumber))
            return false;
        return true;
    }

    // CHECKSTYLE:ON

    @Override
    public final String toString() {
        return streamName + "-" + eventNumber;
    }

}
