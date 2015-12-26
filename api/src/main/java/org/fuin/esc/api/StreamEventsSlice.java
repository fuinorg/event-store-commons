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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.fuin.objects4j.common.Immutable;

/**
 *
 */
@Immutable
public final class StreamEventsSlice {

    private final int fromEventNumber;

    private final int nextEventNumber;

    private final boolean endOfStream;

    private final List<CommonEvent> events;

    /**
     * Constructor with all data.
     * 
     * @param fromEventNumber
     *            The starting point (represented as a sequence number) of the
     *            read.
     * @param events
     *            The events read. The list is internally copied to avoid
     *            external dependencies.
     * @param nextEventNumber
     *            The next event number that can be read.
     * @param endOfStream
     *            Determines whether or not this is the end of the stream.
     */
    public StreamEventsSlice(final int fromEventNumber,
            final List<CommonEvent> events, final int nextEventNumber,
            final boolean endOfStream) {

        this.fromEventNumber = fromEventNumber;
        if (events == null || events.size() == 0) {
            this.events = new ArrayList<CommonEvent>();
        } else {
            this.events = new ArrayList<CommonEvent>(events);
        }
        this.nextEventNumber = nextEventNumber;
        this.endOfStream = endOfStream;
    }

    /**
     * Returns the starting point (represented as a sequence number) of the read
     * operation.
     * 
     * @return Event number.
     */
    public int getFromEventNumber() {
        return fromEventNumber;
    }

    /**
     * Returns the events read.
     * 
     * @return Unmodifiable list of events.
     */
    @NotNull
    public List<CommonEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Returns the next event number that can be read.
     * 
     * @return Next event number.
     */
    public int getNextEventNumber() {
        return nextEventNumber;
    }

    /**
     * Returns a boolean representing whether or not this is the end of the
     * stream.
     * 
     * @return TRUE if this is the end of the stream, else FALSE.
     */
    public boolean isEndOfStream() {
        return endOfStream;
    }

    // CHECKSTYLE:OFF Generated code

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (endOfStream ? 1231 : 1237);
        result = prime * result + fromEventNumber;
        result = prime * result + nextEventNumber;
        result = prime * result
                + ((events == null) ? 0 : Arrays.hashCode(events.toArray()));
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof StreamEventsSlice))
            return false;
        StreamEventsSlice other = (StreamEventsSlice) obj;
        if (endOfStream != other.endOfStream)
            return false;
        if (fromEventNumber != other.fromEventNumber)
            return false;
        if (nextEventNumber != other.nextEventNumber)
            return false;
        if (events == null) {
            if (other.events != null) {
                return false;
            }
        } else {
            if (other.events == null) {
                return false;
            }
            if (!Arrays.equals(events.toArray(), other.events.toArray())) {
                return false;
            }
        }
        return true;
    }

    // CHECKSTYLE:ON

    /**
     * Returns a debug string representation with all data.
     * 
     * @return Includes list content.
     */
    public final String toDebugString() {
        return new ToStringBuilder(this)
                .append("fromEventNumber", fromEventNumber)
                .append("nextEventNumber", nextEventNumber)
                .append("endOfStream", endOfStream)
                .append("events", events).toString();
    }
    
    @Override
    public final String toString() {
        return new ToStringBuilder(this)
                .append("fromEventNumber", fromEventNumber)
                .append("nextEventNumber", nextEventNumber)
                .append("endOfStream", endOfStream)
                .append("events.size", events.size()).toString();
    }

}
