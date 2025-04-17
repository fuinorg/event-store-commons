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

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Immutable;
import org.fuin.objects4j.common.ValueObject;

import java.io.Serializable;

/**
 * Helper class that allows sending an event as XML directly to the event store.
 * The event is uniquely identified by a UUID. It's equals and hash code methods
 * are defined on the <code>id</code>. This class might be useful for tests.
 * It's not used in the 'esc-spi' code itself
 */
@Immutable
@XmlRootElement(name = "event")
public final class Event implements Serializable, ValueObject {

    private static final long serialVersionUID = 1000L;

    /** The ID of the event, used as part of the idempotent write check. */
    @NotNull
    @XmlAttribute(name = "id")
    private EventId id;

    /** The event data. */
    @NotNull
    @XmlElement(name = "data")
    private Data data;

    /** The meta data. */
    @XmlElement(name = "meta")
    private Data meta;

    /**
     * Protected constructor for deserialization.
     */
    protected Event() { //NOSONAR Ignore uninitialized fields
        super();
    }

    /**
     * Constructor without XML meta data.
     * 
     * @param id
     *            The ID of the event, used as part of the idempotent write
     *            check.
     * @param data
     *            Event data.
     * 
     */
    public Event(@NotNull final EventId id, @NotNull final Data data) {
        this(id, data, null);
    }

    /**
     * Constructor with XML meta data.
     * 
     * @param id
     *            The ID of the event, used as part of the idempotent write
     *            check.
     * @param data
     *            Event data.
     * @param meta
     *            Meta data.
     * 
     */
    public Event(@NotNull final EventId id, @NotNull final Data data,
            @Nullable final Data meta) {//NOSONAR 
        super();

        Contract.requireArgNotNull("id", id);
        Contract.requireArgNotNull("data", data);

        this.id = id;
        this.data = data;
        this.meta = meta;

    }

    /**
     * Returns the ID of the event, used as part of the idempotent write check.
     * 
     * @return Unique event identifier.
     */
    @NotNull
    public final EventId getId() {
        return id;
    }

    /**
     * Returns the event data.
     * 
     * @return Event data.
     */
    @NotNull
    public final Data getData() {
        return data;
    }

    /**
     * Returns the meta data.
     * 
     * @return Meta data.
     */
    @Nullable
    public final Data getMeta() {
        return meta;
    }

    /**
     * Returns this object as a common event object.
     * 
     * @param ctx
     *            In case the XML JAXB unmarshalling is used, you have to pass
     *            the JAXB context here.
     * 
     * @return Converted object.
     */
    public final CommonEvent asCommonEvent(final JAXBContext ctx) {
        final Object m;
        if (getMeta() == null) {
            m = null;
        } else {
            m = getMeta().unmarshalContent(ctx);
        }
        final Object d = getData().unmarshalContent(ctx);
        if (getMeta() == null) {
            return new SimpleCommonEvent(getId(),
                    new TypeName(getData().getType()), d);
        }
        return new SimpleCommonEvent(getId(), new TypeName(getData().getType()),
                d, new TypeName(getMeta().getType()), m);
    }

    // CHECKSTYLE:OFF Generated code

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Event))
            return false;
        Event other = (Event) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    // CHECKSTYLE:ON

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("data", data)
                .append("meta", meta).toString();
    }

    /**
     * Creates an event using a common event.
     * 
     * @param selEvent
     *            Event to copy.
     * 
     * @return New instance.
     */
    public static Event valueOf(final CommonEvent selEvent) {
        final Data data = Data.valueOf(selEvent.getDataType().asBaseType(),
                selEvent.getData());
        if (selEvent.getMeta() == null) {
            return new Event(selEvent.getId(), data);
        }
        final Data meta = Data.valueOf("meta", selEvent.getMeta());
        return new Event(selEvent.getId(), data, meta);
    }

}
