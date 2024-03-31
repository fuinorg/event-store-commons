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
package org.fuin.esc.jaxb;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.fuin.esc.api.HasSerializedDataTypeConstant;
import org.fuin.esc.api.IEscEvent;
import org.fuin.objects4j.common.Contract;

import java.util.UUID;

/**
 * An event structure.
 */
@HasSerializedDataTypeConstant
@XmlRootElement(name = IEscEvent.EL_ROOT_NAME)
public final class EscEvent implements IEscEvent {

    @XmlElement(name = IEscEvent.EL_EVENT_ID)
    private String eventId;

    @XmlElement(name = IEscEvent.EL_EVENT_TYPE)
    private String eventType;

    @XmlElement(name = IEscEvent.EL_DATA)
    private DataWrapper data;

    @XmlElement(name = IEscEvent.EL_META_DATA)
    private DataWrapper meta;

    /**
     * Default constructor for JAXB.
     */
    protected EscEvent() {
        super();
    }

    /**
     * Constructor with mandatory data.
     *
     * @param eventId   Unique event identifier.
     * @param eventType Unique type name of the event.
     * @param data      The data.
     */
    public EscEvent(@NotNull final UUID eventId, @NotNull final String eventType, @NotNull final DataWrapper data) {
        this(eventId, eventType, data, null);
    }

    /**
     * Constructor with all data.
     *
     * @param eventId   Unique event identifier.
     * @param eventType Unique type name of the event.
     * @param data      The data.
     * @param meta      The meta data if available.
     */
    public EscEvent(@NotNull final UUID eventId, @NotNull final String eventType, @NotNull final DataWrapper data,
                    @Nullable final DataWrapper meta) {
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
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns the unique type name of the event.
     *
     * @return Event type.
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Returns the data.
     *
     * @return Data.
     */
    public DataWrapper getData() {
        return data;
    }

    /**
     * Returns the metadata.
     *
     * @return Metadata.
     */
    public DataWrapper getMeta() {
        return meta;
    }

}
