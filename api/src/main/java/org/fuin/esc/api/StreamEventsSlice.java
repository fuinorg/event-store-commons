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

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Multiple ordered events read from an eventstore.
 */
@Immutable
public final class StreamEventsSlice {

    private final long fromEventNumber;

    private final long nextEventNumber;

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
     *            Determines whether this is the end of the stream.
     */
    public StreamEventsSlice(final long fromEventNumber,
                             @Nullable final List<CommonEvent> events, final long nextEventNumber,
                             final boolean endOfStream) {

        this.fromEventNumber = fromEventNumber;
        if (events == null || events.isEmpty()) {
            this.events = new ArrayList<>();
        } else {
            this.events = new ArrayList<>(events);
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
    public long getFromEventNumber() {
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
    public long getNextEventNumber() {
        return nextEventNumber;
    }

    /**
     * Returns a boolean representing whether this is the end of the
     * stream.
     *
     * @return TRUE if this is the end of the stream, else FALSE.
     */
    public boolean isEndOfStream() {
        return endOfStream;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (endOfStream ? 1231 : 1237);
        result = prime * result + (int) (fromEventNumber ^ (fromEventNumber >>> 32));
        result = prime * result + (int) (nextEventNumber ^ (nextEventNumber >>> 32));
        result = prime * result
                + ((events == null) ? 0 : Arrays.hashCode(events.toArray()));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof StreamEventsSlice other))
            return false;
        if (endOfStream != other.endOfStream)
            return false;
        if (fromEventNumber != other.fromEventNumber)
            return false;
        if (nextEventNumber != other.nextEventNumber)
            return false;
        if (events == null) {
            return other.events == null;
        } else {
            if (other.events == null) {
                return false;
            }
            return Arrays.equals(events.toArray(), other.events.toArray());
        }
    }

    @Override
    public String toString() {
        return "StreamEventsSlice{" +
                "fromEventNumber=" + fromEventNumber +
                ", nextEventNumber=" + nextEventNumber +
                ", endOfStream=" + endOfStream +
                ", events.size=" + events.size() +
                '}';
    }

}
