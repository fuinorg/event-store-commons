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
package org.fuin.esc.jackson;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.HasSerializedDataTypeConstant;
import org.fuin.esc.api.IDataWrapper;
import org.fuin.esc.api.IEscEvent;
import org.fuin.objects4j.common.Contract;

import java.util.UUID;

/**
 * An event structure.
 */
@HasSerializedDataTypeConstant
public final class EscEvent implements IEscEvent {

    private String eventId;

    private String eventType;

    private DataWrapper data;

    private DataWrapper meta;

    /**
     * Default constructor for Jackson.
     */
    protected EscEvent() {
        super();
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
     *            The meta data, if available.
     */
    public EscEvent(@NotNull final UUID eventId,
                    @NotNull final String eventType,
                    @NotNull final DataWrapper data,
                    @NotNull final DataWrapper meta) {
        super();
        Contract.requireArgNotNull("eventId", eventId);
        Contract.requireArgNotNull("eventType", eventType);
        Contract.requireArgNotNull("data", data);
        Contract.requireArgNotNull("meta", meta);
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
     * Sets the unique event identifier.
     *
     * @param eventId Event ID.
     */
    void setEventId(@NotNull final String eventId) {
        Contract.requireArgNotNull("eventId", eventId);
        this.eventId = eventId;
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
     * Sets the unique type name of the event.
     *
     * @param eventType Event type.
     */
    void setEventType(@NotNull final String eventType) {
        Contract.requireArgNotNull("eventType", eventType);
        this.eventType = eventType;
    }

    /**
     * Returns the data.
     *
     * @return Data.
     */
    public IDataWrapper getData() {
        return data;
    }

    /**
     * Sets the data.
     *
     * @param data Data.
     */
    void setData(DataWrapper data) {
        Contract.requireArgNotNull("data", data);
        this.data = data;
    }

    /**
     * Returns the metadata.
     *
     * @return Metadata.
     */
    @NotNull
    public IDataWrapper getMeta() {
        return meta;
    }

    /**
     * Sets the metadata.
     *
     * @param meta Metadata.
     */
    void setMeta(@NotNull final DataWrapper meta) {
        Contract.requireArgNotNull("meta", meta);
        this.meta = meta;
    }

}
