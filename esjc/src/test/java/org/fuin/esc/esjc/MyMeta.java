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
package org.fuin.esc.esjc;

import java.io.Serializable;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerDeserializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.ToJsonCapable;
import javax.annotation.Nullable;

/**
 * Example meta data. .
 */
@XmlRootElement(name = "MyMeta")
public final class MyMeta implements Serializable, ToJsonCapable {

    private static final long serialVersionUID = 100L;

    /** Unique name of the meta type. */
    public static final TypeName TYPE = new TypeName("MyMeta");

    /** Unique name of the serialized meta type. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    private static final String USER = "user";

    @XmlElement(name = USER)
    private String user;

    /**
     * Protected default constructor for JAXB.
     */
    protected MyMeta() {
        super();
    }

    /**
     * Constructor with all mandatory data.
     * 
     * @param user
     *            User ID.
     */
    public MyMeta(@Nullable final String user) {
        super();
        this.user = user;
    }

    /**
     * Returns the user.
     * 
     * @return User ID.
     */
    public final String getUser() {
        return user;
    }

    // CHECKSTYLE:OFF Generated code

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((user == null) ? 0 : user.hashCode());
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
        MyMeta other = (MyMeta) obj;
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        return true;
    }

    // CHECKSTYLE:ON

    @Override
    public JsonStructure toJson() {
        return Json.createObjectBuilder().add(USER, user).build();
    }

    @Override
    public final String toString() {
        return "My meta: " + user;
    }

    /**
     * Creates in instance from the given JSON object.
     * 
     * @param jsonObj
     *            Object to read values from.
     * 
     * @return New instance.
     */
    public static MyMeta create(final JsonObject jsonObj) {
        final String user = jsonObj.getString(USER);
        return new MyMeta(user);
    }

    /**
     * Serializes and deserializes a {@link MyMeta} object as JSON. The content
     * type for serialization is always "application/json".
     */
    public static class MyMetaJsonDeSerializer implements SerDeserializer {

        private JsonDeSerializer jsonDeSer;

        /**
         * Constructor with UTF-8 encoding.
         */
        public MyMetaJsonDeSerializer() {
            super();
            this.jsonDeSer = new JsonDeSerializer();
        }

        /**
         * Constructor with type and encoding.
         * 
         * @param encoding
         *            Default encoding to use.
         */
        public MyMetaJsonDeSerializer(final Charset encoding) {
            super();
            this.jsonDeSer = new JsonDeSerializer(encoding);
        }

        @Override
        public final EnhancedMimeType getMimeType() {
            return jsonDeSer.getMimeType();
        }

        @Override
        public final <T> byte[] marshal(final T obj) {
            return jsonDeSer.marshal(obj);
        }

        @SuppressWarnings("unchecked")
        @Override
        public final MyMeta unmarshal(final Object data, final EnhancedMimeType mimeType) {
            final JsonObject jsonObj = jsonDeSer.unmarshal(data, mimeType);
            return MyMeta.create(jsonObj);
        }

    }
    
}
