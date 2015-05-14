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
package org.fuin.esc.intf;

import javax.activation.MimeType;
import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Immutable;
import org.fuin.objects4j.common.NeverNull;
import org.fuin.objects4j.vo.UUIDStr;

/**
 * Event that is uniquely identified by a UUID. It's equals and hash code
 * methods are defined on the <code>id</code>.
 */
@Immutable
public final class EventData {

    /**
     * The ID of the event, used as part of the idempotent write check. This is
     * type string to allow different UUID implementations. It has to be a valid
     * UUID string representation.
     */
    @NotNull
    @UUIDStr
    private final String id;

    /**
     * Type of the event that describes the event uniquely within the list of
     * all events.
     */
    @NotNull
    private final String type;

    /** Mime type of the data. */
    @NotNull
    private final MimeType mimeType;

    /** Data in format defined by the mime type. */
    @NotNull
    private final byte[] data;

    /**
     * Constructor with all mandatory data.
     * 
     * @param id
     *            The ID of the event, used as part of the idempotent write
     *            check. This is type string to allow different UUID
     *            implementations. It has to be a valid UUID string
     *            representation.
     * @param type
     *            Type of the event that describes the event uniquely within the
     *            list of all events.
     * @param mimeType
     *            Mime type of the data.
     * @param data
     *            Data in format defined by the mime type.
     */
    public EventData(@NotNull @UUIDStr final String id,
            @NotNull final String type, @NotNull final MimeType mimeType,
            @NotNull final byte[] data) {
        super();

        Contract.requireArgNotNull("eventId", id);
        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("mimeType", mimeType);
        Contract.requireArgNotNull("data", data);

        this.id = id;
        this.type = type;
        this.mimeType = mimeType;
        this.data = data;
    }

    /**
     * Returns the ID of the event, used as part of the idempotent write check.
     * This is type string to allow different UUID implementations. It has to be
     * a valid UUID string representation.
     * 
     * @return Unique event identifier.
     */
    @NeverNull
    public final String getId() {
        return id;
    }

    /**
     * Returns the type of the event that describes the event uniquely within
     * the list of all events.
     * 
     * @return Unique and never changing type name.
     */
    @NeverNull
    public final String getType() {
        return type;
    }

    /**
     * Returns the Internet Media Type that classifies the data.
     * 
     * @return Mime type.
     */
    @NeverNull
    public final MimeType getMimeType() {
        return mimeType;
    }

    /**
     * Returns the raw data block.
     * 
     * @return Event data.
     */
    @NeverNull
    public final byte[] getData() {
        return data;
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
        if (!(obj instanceof EventData))
            return false;
        EventData other = (EventData) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    // CHECKSTYLE:ON

}
