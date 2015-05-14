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

import javax.json.JsonObject;
import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Immutable;
import org.fuin.objects4j.common.NeverNull;
import org.fuin.objects4j.common.Nullable;
import org.fuin.objects4j.vo.UUIDStr;
import org.w3c.dom.Document;

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
    private String id;

    /**
     * Type of the event that describes the event uniquely within the list of
     * all events.
     */
    @NotNull
    private String type;

    /** Mime type of the data. */
    @NotNull
    private VersionedMimeType mimeType;

    /** Data in format defined by the mime type. */
    @NotNull
    private byte[] data;

    private JsonObject metaJson;

    private Document metaDom;

    /**
     * Constructor with XML meta data.
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
     * @param metaDom
     *            Meta data expressed as XML data.
     * 
     */
    public EventData(@NotNull @UUIDStr final String id,
            @NotNull final String type,
            @NotNull final VersionedMimeType mimeType,
            @NotNull final byte[] data, @Nullable final Document metaDom) {
        this(id, type, mimeType, data, null, metaDom);
    }

    /**
     * Constructor with JSON meta data.
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
     * @param metaJson
     *            Meta data expressed as JSON data.
     * 
     */
    public EventData(@NotNull @UUIDStr final String id,
            @NotNull final String type,
            @NotNull final VersionedMimeType mimeType,
            @NotNull final byte[] data, @Nullable final JsonObject metaJson) {
        this(id, type, mimeType, data, metaJson, null);
    }

    /**
     * Constructor without meta data.
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
            @NotNull final String type,
            @NotNull final VersionedMimeType mimeType,
            @NotNull final byte[] data) {
        this(id, type, mimeType, data, null, null);
    }

    private EventData(@NotNull @UUIDStr final String id,
            @NotNull final String type,
            @NotNull final VersionedMimeType mimeType,
            @NotNull final byte[] data, @Nullable final JsonObject metaJson,
            final Document metaDom) {
        super();

        Contract.requireArgNotNull("eventId", id);
        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("mimeType", mimeType);
        Contract.requireArgNotNull("data", data);

        this.id = id;
        this.type = type;
        this.mimeType = mimeType;
        this.data = data;
        this.metaJson = metaJson;
        this.metaDom = metaDom;

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
    public final VersionedMimeType getMimeType() {
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

    /**
     * Meta data expressed as JSON object. Either
     * {@link EventData#getMetaJson()} or {@link #getMetaDom()} may be non-
     * <code>null</code>. This means only one of them and never both can have a
     * non-<code>null</code> value.
     * 
     * @return The meta data.
     */
    @Nullable
    public final JsonObject getMetaJson() {
        return metaJson;
    }

    /**
     * Meta data expressed as XML document. Either
     * {@link EventData#getMetaJson()} or {@link #getMetaDom()} may be non-
     * <code>null</code>. This means only one of them and never both can have a
     * non-<code>null</code> value.
     * 
     * @return The meta data.
     */
    @Nullable
    public final Document getMetaDom() {
        return metaDom;
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
