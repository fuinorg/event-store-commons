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
package org.fuin.esc.spi;

import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;

/**
 * An event structure.
 */
@XmlRootElement(name = "Event")
public final class EscEvent {

    /** Unique name of the event. */
    public static final TypeName TYPE = new TypeName("Event");

    /** Unique name of the event. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());    
    
    @XmlElement(name = "EventId")
    private String eventId;

    @XmlElement(name = "EventType")
    private String eventType;

    @XmlElement(name = "Data")
    private DataWrapper data;

    @XmlElement(name = "MetaData")
    private EscMetaData meta;

    /**
     * Default constructor for JAXB.
     */
    protected EscEvent() {
        super();
    }

    /**
     * Constructor with mandatory data.
     * 
     * @param eventId
     *            Unique event identifier.
     * @param eventType
     *            Unique type name of the event.
     * @param data
     *            The data.
     */
    public EscEvent(@NotNull final UUID eventId, @NotNull final String eventType,
            @NotNull final DataWrapper data) {
        this(eventId, eventType, data, null);
    }

    /**
     * Constructor with all data.
     * 
     * @param eventId
     *            Unique event identifier.
     * @param eventType
     *            Unique type name of the event.
     * @param data
     *            The data.
     * @param meta
     *            The meta data if available.
     */
    public EscEvent(@NotNull final UUID eventId, @NotNull final String eventType,
            @NotNull final DataWrapper data, @Nullable final EscMetaData meta) {
        super();
        Contract.requireArgNotNull("eventId", eventId);
        Contract.requireArgNotNull("eventType", eventType);
        Contract.requireArgNotNull("data", data);
        this.eventId = eventId.toString();
        this.eventType = eventType;
        this.data = data;
        this.meta = meta;
    }

    /**
     * Returns the unique event identifier.
     * 
     * @return Event ID.
     */
    public final String getEventId() {
        return eventId;
    }

    /**
     * Returns the unique type name of the event.
     * 
     * @return Event type.
     */
    public final String getEventType() {
        return eventType;
    }

    /**
     * Returns the data.
     * 
     * @return Data.
     */
    public final DataWrapper getData() {
        return data;
    }

    /**
     * Returns the meta data.
     * 
     * @return Meta data.
     */
    public final EscMetaData getMeta() {
        return meta;
    }

}
