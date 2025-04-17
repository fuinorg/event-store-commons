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
package org.fuin.esc.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.StreamEventsSlice;

import javax.annotation.concurrent.Immutable;

/**
 * A slice of data from a stream.
 */
@Immutable
@XmlRootElement(name = "slice")
public final class Slice implements Serializable {

    private static final long serialVersionUID = 1000L;

    @XmlAttribute(name = "from-stream-no")
    private long fromEventNumber;

    @XmlAttribute(name = "next-stream-no")
    private long nextEventNumber;

    @XmlAttribute(name = "end-of-stream")
    private boolean endOfStream;

    @XmlElement(name = "event")
    private List<Event> events;

    /**
     * Protected constructor for deserialization.
     */
    protected Slice() {
        super();
    }

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
    public Slice(final long fromEventNumber, final List<Event> events,
                 final long nextEventNumber, final boolean endOfStream) {

        this.fromEventNumber = fromEventNumber;
        if (events == null || events.size() == 0) {
            this.events = new ArrayList<Event>();
        } else {
            this.events = new ArrayList<Event>(events);
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
    public List<Event> getEvents() {
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
     * Returns a boolean representing whether or not this is the end of the
     * stream.
     *
     * @return TRUE if this is the end of the stream, else FALSE.
     */
    public boolean isEndOfStream() {
        return endOfStream;
    }


    @Override
    public final int hashCode() {
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
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Slice))
            return false;
        Slice other = (Slice) obj;
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


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("fromEventNumber", fromEventNumber)
                .append("nextEventNumber", nextEventNumber)
                .append("endOfStream", endOfStream).append("events", events)
                .toString();
    }

    /**
     * Creates a slice from a stream event slice.
     *
     * @param sel
     *            Slice to copy.
     *
     * @return Copied information with changed type.
     */
    public static Slice valueOf(final StreamEventsSlice sel) {
        final List<Event> events = new ArrayList<Event>();
        final List<CommonEvent> selEvents = sel.getEvents();
        for (final CommonEvent selEvent : selEvents) {
            events.add(Event.valueOf(selEvent));
        }
        return new Slice(sel.getFromEventNumber(), events,
                sel.getNextEventNumber(), sel.isEndOfStream());
    }

}
