/**
 * Copyright (C) 2013 Future Invent Informationsmanagement GmbH. All rights
 * reserved. <http://www.fuin.org/>
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
package org.fuin.esc.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.fuin.esc.api.CommonEvent;
import org.fuin.objects4j.common.NeverNull;

/**
 * Multiple events.
 */
@XmlRootElement(name = "events")
public final class Events implements Serializable {

    private static final long serialVersionUID = 1000L;

    @XmlElement(name = "event")
    private List<Event> events;

    /**
     * Default constructor
     */
    public Events() {
        super();
        this.events = new ArrayList<Event>();
    }

    /**
     * Constructor with all data.
     * 
     * @param events
     *            The events read. The list is internally copied to avoid
     */
    public Events(final List<Event> events) {
        this();
        append(events);
    }

    /**
     * Appends events to the list.
     * 
     * @param events
     *            Events to add.
     */
    public void append(final List<Event> events) {
        if (events != null && events.size() > 0) {
            this.events.addAll(events);
        }
    }

    /**
     * Appends events to the list.
     * 
     * @param events
     *            Events to add.
     */
    public void append(final Event... events) {
        if (events != null && events.length > 0) {
            this.events.addAll(Arrays.asList(events));
        }
    }

    /**
     * Returns the events read.
     * 
     * @return Unmodifiable list of events.
     */
    @NeverNull
    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Returns this object as a list of common event objects.
     * 
     * @param classesToBeBound In case the XML JAXB unmarshalling is used, you have to pass the classes for the content here.
     * 
     * @return Converted list.
     */
    public List<CommonEvent> asCommonEvents(final Class<?>... classesToBeBound) {
        final List<CommonEvent> list = new ArrayList<CommonEvent>();
        for (final Event event : events) {
            final Object meta = event.getMeta().unmarshalContent(classesToBeBound);
            final Object data = event.getData().unmarshalContent(classesToBeBound);
            list.add(new CommonEvent(event.getId(), event.getData().getType(),
                    data, meta));
        }
        return list;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("events", events).toString();
    }

}
