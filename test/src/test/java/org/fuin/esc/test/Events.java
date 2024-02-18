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

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.spi.EscSpiUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class that allows sending multiple events as XML directly to the event
 * store. It a wrapper to allow an XML list of events. This class might be
 * useful for tests. It's not used in the 'esc-spi' code itself
 */
@XmlRootElement(name = "events")
public final class Events implements Serializable {

    @Serial
    private static final long serialVersionUID = 1000L;

    @XmlElement(name = "event")
    private List<Event> events;

    /**
     * Default constructor.
     */
    public Events() {
        super();
        this.events = new ArrayList<>();
    }

    /**
     * Constructor with all data.
     *
     * @param events The events read. The list is internally copied to avoid
     */
    public Events(final List<Event> events) {
        this();
        append(events);
    }

    /**
     * Appends events to the list.
     *
     * @param events Events to add.
     */
    public void append(final List<Event> events) {
        if (events != null && !events.isEmpty()) {
            this.events.addAll(events);
        }
    }

    /**
     * Appends events to the list.
     *
     * @param events Events to add.
     */
    public void append(final Event... events) {
        if (events != null && events.length > 0) {
            this.events.addAll(EscSpiUtils.asList(events));
        }
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
     * Returns this object as a list of common event objects.
     *
     * @param ctx In case the XML JAXB unmarshalling is used, you have to pass
     *            the JAXB context here.
     * @return Converted list.
     */
    public List<CommonEvent> asCommonEvents(final JAXBContext ctx) {
        final List<CommonEvent> list = new ArrayList<>();
        for (final Event event : events) {
            list.add(event.asCommonEvent(ctx));
        }
        return list;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("events", events).toString();
    }

}
