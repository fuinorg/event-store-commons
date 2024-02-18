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

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;

/**
 * Connects the stream with the event entries.
 */
@MappedSuperclass
public abstract class JpaStreamEvent {

    /** SQL EVENT ID column name. */
    public static final String COLUMN_EVENTS_ID = "events_id";

    /** Column event number - Defined in subclasses as part of the composite identifier. */
    public static final String COLUMN_EVENT_NUMBER = "event_number";


    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = COLUMN_EVENTS_ID, nullable = false, updatable = false)
    private JpaEvent event;

    /**
     * Protected default constructor only required for JPA.
     */
    protected JpaStreamEvent() { //NOSONAR Ignore uninitialized fields
        super();
    }

    /**
     * Constructs a stream event.
     *
     * @param event
     *            Event to be connected with this event stream.
     */
    public JpaStreamEvent(@NotNull final JpaEvent event) {
        super();
        Contract.requireArgNotNull("eventEntry", event);
        this.event = event;
    }

    /**
     * Returns the actual event.
     *
     * @return Event connected with this stream
     */
    @NotNull
    public JpaEvent getEvent() {
        return event;
    }

}
