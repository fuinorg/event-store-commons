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
package org.fuin.esc.eshttp;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.ToJsonCapable;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;

/**
 * An event structure.
 */
@XmlRootElement(name = EscEvent.EL_ROOT_NAME)
public final class EscEvent implements ToJsonCapable {

    /** Unique XML/JSON root element name of the type. */
    public static final String EL_ROOT_NAME = "Event";

    private static final String EL_EVENT_ID = "EventId";

    private static final String EL_EVENT_TYPE = "EventType";

    private static final String EL_DATA = "Data";

    private static final String EL_META_DATA = "MetaData";

    /** Unique name of the type. */
    public static final TypeName TYPE = new TypeName(EL_ROOT_NAME);

    /** Unique name of the serialized type. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    @XmlElement(name = EL_EVENT_ID)
    private String eventId;

    @XmlElement(name = EL_EVENT_TYPE)
    private String eventType;

    @XmlElement(name = EL_DATA)
    private DataWrapper data;

    @XmlElement(name = EL_META_DATA)
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
     * @param eventId
     *            Unique event identifier.
     * @param eventType
     *            Unique type name of the event.
     * @param data
     *            The data.
     */
    public EscEvent(@NotNull final UUID eventId, @NotNull final String eventType, @NotNull final DataWrapper data) {
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
    public final DataWrapper getMeta() {
        return meta;
    }

    @Override
    public final JsonObject toJson() {
        return Json.createObjectBuilder().add(EL_EVENT_ID, eventId).add(EL_EVENT_TYPE, eventType)
                .add(EL_DATA, data.toJson()).add(EL_META_DATA, meta.toJson()).build();
    }

    @Override
    public String getRootElementName() {
        return EL_ROOT_NAME;
    }

    /**
     * Creates in instance from the given JSON object.
     * 
     * @param jsonObj
     *            Object to read values from.
     * 
     * @return New instance.
     */
    public static EscEvent create(final JsonObject jsonObj) {
        final String eventId = jsonObj.getString(EL_EVENT_ID);
        final String eventType = jsonObj.getString(EL_EVENT_TYPE);
        final JsonObject jsonData = jsonObj.getJsonObject(EL_DATA);
        final JsonObject jsonMeta = jsonObj.getJsonObject(EL_META_DATA);
        return new EscEvent(UUID.fromString(eventId), eventType, new DataWrapper(jsonData), 
                new DataWrapper(jsonMeta));
    }

}
