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
package org.fuin.esc.esgrpc;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.JsonDeSerializer;
import org.fuin.esc.api.SerDeserializer;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.ToJsonCapable;
import org.fuin.objects4j.common.Contract;

/**
 * Something interesting happened. Equals and hash code are based on the UUID.
 */
@XmlRootElement(name = "MyEvent")
public final class MyEvent implements Serializable, ToJsonCapable {

    private static final long serialVersionUID = 100L;

    /** Unique name of the event. */
    public static final TypeName TYPE = new TypeName("MyEvent");

    /** Unique name of the serialized event. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    private static final String ID = "id";

    private static final String DESCRIPTION = "description";

    @XmlElement(name = ID)
    private String id;

    @XmlElement(name = DESCRIPTION)
    private String description;

    /**
     * Protected default constructor for JAXB.
     */
    protected MyEvent() {
        super();
    }

    /**
     * Constructor with random UUID.
     * 
     * @param description
     *            The description.
     */
    public MyEvent(@NotEmpty final String description) {
        super();
        Contract.requireArgNotEmpty("description", description);
        this.id = UUID.randomUUID().toString();
        this.description = description;
    }

    /**
     * Constructor with all mandatory data.
     * 
     * @param uuid
     *            The unique identifier of the event.
     * @param description
     *            The description.
     */
    public MyEvent(@NotNull final UUID uuid, @NotEmpty final String description) {
        super();
        Contract.requireArgNotNull("uuid", uuid);
        Contract.requireArgNotEmpty("description", description);
        this.id = uuid.toString();
        this.description = description;
    }

    /**
     * Returns the unique identifier.
     * 
     * @return UUID string.
     */
    @NotNull
    public final String getId() {
        return id;
    }

    /**
     * Returns the description.
     * 
     * @return The description.
     */
    @NotEmpty
    public final String getDescription() {
        return description;
    }

    // CHECKSTYLE:OFF Generated code

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MyEvent other = (MyEvent) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    // CHECKSTYLE:ON

    @Override
    public JsonStructure toJson() {
        return Json.createObjectBuilder().add(ID, id).add(DESCRIPTION, description).build();
    }

    @Override
    public final String toString() {
        return "My event: " + description;
    }

    /**
     * Creates in instance from the given JSON object.
     * 
     * @param jsonObj
     *            Object to read values from.
     * 
     * @return New instance.
     */
    public static MyEvent create(final JsonObject jsonObj) {
        final String id = jsonObj.getString(ID);
        final String description = jsonObj.getString(DESCRIPTION);
        return new MyEvent(UUID.fromString(id), description);
    }

    /**
     * Serializes and deserializes a {@link MyEvent} object as JSON. The content
     * type for serialization is always "application/json".
     */
    public static class MyEventJsonDeSerializer implements SerDeserializer {

        private JsonDeSerializer jsonDeSer;

        /**
         * Constructor with UTF-8 encoding.
         */
        public MyEventJsonDeSerializer() {
            super();
            this.jsonDeSer = new JsonDeSerializer();
        }

        /**
         * Constructor with type and encoding.
         * 
         * @param encoding
         *            Default encoding to use.
         */
        public MyEventJsonDeSerializer(final Charset encoding) {
            super();
            this.jsonDeSer = new JsonDeSerializer(encoding);
        }

        @Override
        public final EnhancedMimeType getMimeType() {
            return jsonDeSer.getMimeType();
        }

        @Override
        public final <T> byte[] marshal(final T obj, final SerializedDataType type) {
            return jsonDeSer.marshal(obj, type);
        }

        @SuppressWarnings("unchecked")
        @Override
        public final MyEvent unmarshal(final Object data, final SerializedDataType type, final EnhancedMimeType mimeType) {
            final JsonObject jsonObj = jsonDeSer.unmarshal(data, type, mimeType);
            return MyEvent.create(jsonObj);
        }

    }
    
}
