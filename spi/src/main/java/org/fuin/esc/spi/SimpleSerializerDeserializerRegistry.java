/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
package org.fuin.esc.spi;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Contract;

/**
 * Contains all known serializers and deserializers.
 */
public final class SimpleSerializerDeserializerRegistry implements
        SerializerRegistry, DeserializerRegistry {

    private final Map<String, Serializer> serMap;

    private final Map<Key, Deserializer> desMap;

    private final Map<String, EnhancedMimeType> contentTypes;

    /**
     * Default constructor.
     */
    public SimpleSerializerDeserializerRegistry() {
        super();
        serMap = new HashMap<String, Serializer>();
        desMap = new HashMap<Key, Deserializer>();
        contentTypes = new HashMap<String, EnhancedMimeType>();
    }

    /**
     * Adds a new deserializer to the registry.
     * 
     * @param type
     *            Type of the data.
     * @param contentType
     *            Content type like "application/xml" or "application/json"
     *            (without parameters - Only base type).
     * @param deserializer
     *            Deserializer.
     */
    public final void addDeserializer(@NotNull final String type,
            final String contentType, @NotNull final Deserializer deserializer) {

        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("contentType", contentType);
        Contract.requireArgNotNull("deserializer", deserializer);

        final Key key = new Key(type, contentType);
        desMap.put(key, deserializer);

    }

    /**
     * Sets the default content type to use if no content type is given.
     * 
     * @param type
     *            Type of the data.
     * @param contentType
     *            Content type like "application/xml" or "application/json"
     *            (without parameters - Only base type).
     */
    public final void setDefaultContentType(@NotNull final String type,
            final EnhancedMimeType contentType) {

        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("contentType", contentType);

        contentTypes.put(type, contentType);

    }

    /**
     * Adds a new serializer to the registry.
     * 
     * @param type
     *            Type of the data.
     * @param serializer
     *            Serializer.
     */
    public final void addSerializer(@NotNull final String type,
            @NotNull final Serializer serializer) {

        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("serializer", serializer);

        serMap.put(type, serializer);

    }

    @Override
    public Serializer getSerializer(final String type) {
        Contract.requireArgNotNull("type", type);
        return serMap.get(type);
    }

    @Override
    public final Deserializer getDeserializer(final String type,
            final EnhancedMimeType mimeType) {

        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("mimeType", mimeType);

        final Key key = new Key(type, mimeType.getBaseType());
        return desMap.get(key);
    }

    @Override
    public final Deserializer getDeserializer(final String type) {
        Contract.requireArgNotNull("type", type);

        final EnhancedMimeType contentType = contentTypes.get(type);
        if (contentType == null) {
            throw new IllegalArgumentException(
                    "No default content type was set for: " + type);
        }

        final Key key = new Key(type, contentType.getBaseType());
        return desMap.get(key);
    }

    @Override
    public final EnhancedMimeType getDefaultMimeType(final String type) {
        Contract.requireArgNotNull("type", type);

        final EnhancedMimeType contentType = contentTypes.get(type);
        if (contentType == null) {
            return null;
        }
        return contentType;
    }

    /**
     * Key used to find an appropriate deserializer.
     */
    private static class Key {

        private final String type;
        private final String contentType;

        public Key(final String type, final String contentType) {
            this.type = type;
            this.contentType = contentType;
        }

        // CHECKSTYLE:OFF Generated code

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result
                    + ((contentType == null) ? 0 : contentType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof Key))
                return false;
            Key other = (Key) obj;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            if (contentType == null) {
                if (other.contentType != null)
                    return false;
            } else if (!contentType.equals(other.contentType))
                return false;
            return true;
        }

        // CHECKSTYLE:ON

    }

}
