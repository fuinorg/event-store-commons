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

import jakarta.annotation.Nullable;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.test.examples.BookAddedEvent;
import org.fuin.objects4j.core.UUIDStrValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Expected chunk when reading forward all events of a stream.
 */
public final class ReadAllForwardChunk {

    // Creation (Initialized by Cucumber)
    // DO NOT CHANGE ORDER OR RENAME VARIABLES!

    private String resultEventId1;

    private String resultEventId2;

    private String resultEventId3;

    private String resultEventId4;

    private String resultEventId5;

    private String resultEventId6;

    private String resultEventId7;

    private String resultEventId8;

    private String resultEventId9;

    // NOT set by Cucumber

    private List<CommonEvent> eventList;

    /**
     * Default constructor used by Cucumber.
     */
    public ReadAllForwardChunk() {
        super();
    }

    /**
     * Constructor for manual creation.
     *
     * @param events
     *            Events.
     */
    public ReadAllForwardChunk(@Nullable final String... events) {
        super();
        eventList = new ArrayList<>();
        if (events != null) {
            for (final String event : events) {
                addEvent(eventList, event);
            }
        }
    }

    /**
     * Constructor for manual creation.
     *
     * @param events
     *            Events.
     */
    public ReadAllForwardChunk(@Nullable final List<CommonEvent> events) {
        super();
        eventList = new ArrayList<>();
        if (events != null) {
            eventList.addAll(events);
        }
    }

    /**
     * Returns a list of expected events.
     *
     * @return Event list.
     */
    public final List<CommonEvent> getEvents() {
        if (eventList == null) {
            eventList = new ArrayList<>();
            addEvent(eventList, resultEventId1);
            addEvent(eventList, resultEventId2);
            addEvent(eventList, resultEventId3);
            addEvent(eventList, resultEventId4);
            addEvent(eventList, resultEventId5);
            addEvent(eventList, resultEventId6);
            addEvent(eventList, resultEventId7);
            addEvent(eventList, resultEventId8);
            addEvent(eventList, resultEventId9);
        }
        return eventList;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((eventList == null) ? 0 : eventList.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ReadAllForwardChunk other = (ReadAllForwardChunk) obj;
        if (eventList == null) {
            if (other.eventList != null) {
                return false;
            }
        } else if (!eventList.equals(other.eventList)) {
            return false;
        }
        return true;
    }

    private static void addEvent(final List<CommonEvent> events,
                                 final String eventId) {
        if (eventId != null && UUIDStrValidator.isValid(eventId)) {
            final CommonEvent ce = new SimpleCommonEvent(new EventId(eventId),
                    BookAddedEvent.TYPE, new BookAddedEvent("Any", "John Doe"));
            events.add(ce);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        if (eventList != null) {
            for (final CommonEvent event : eventList) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(event.getId());
            }
        }
        sb.append("}");
        return sb.toString();
    }

}
