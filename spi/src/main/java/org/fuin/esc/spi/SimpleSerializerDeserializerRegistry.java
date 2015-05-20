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
package org.fuin.esc.spi;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Contract;

/**
 * Contains all known serializers and deserializers.
 */
public final class SimpleSerializerDeserializerRegistry implements
        SerializerDeserializerRegistry {

    private final Map<String, Serializer> serMap;

    private final Map<Key, Deserializer> desMap;

    /**
     * Default constructor.
     */
    public SimpleSerializerDeserializerRegistry() {
        super();
        serMap = new HashMap<String, Serializer>();
        desMap = new HashMap<Key, Deserializer>();
    }

    /**
     * Adds a new deserializer to the registry.
     * 
     * @param type
     *            Type of the data.
     * @param mimeType
     *            Mime type.
     * @param deserializer
     *            Deserializer.
     */
    public final void addDeserializer(@NotNull final String type,
            final VersionedMimeType mimeType,
            @NotNull final Deserializer deserializer) {

        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("mimeType", mimeType);
        Contract.requireArgNotNull("deserializer", deserializer);

        final Key key = new Key(type, mimeType);
        desMap.put(key, deserializer);

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

    /**
     * Convenience method to add an combined XML serializer/deserializer.
     * 
     * @param vcds
     *            Deserializer to add.
     */
    public final void add(@NotNull final SerializerDeserializer sd) {
        Contract.requireArgNotNull("sd", sd);
        this.addSerializer(sd.getType(), sd);
        this.addDeserializer(sd.getType(), sd.getMimeType(), sd);
    }

    @Override
    public Serializer getSerializer(final String type) {
        Contract.requireArgNotNull("type", type);
        return serMap.get(type);
    }

    @Override
    public final Deserializer getDeserializer(final String type,
            final VersionedMimeType mimeType) {

        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("mimeType", mimeType);

        final Key key = new Key(type, mimeType);
        return desMap.get(key);
    }

    /**
     * Key used to find an appropriate deserialize.
     */
    private static class Key {

        private final String type;
        private final VersionedMimeType mimeType;

        public Key(final String type, VersionedMimeType mimeType) {
            this.type = type;
            this.mimeType = mimeType;
        }

        // CHECKSTYLE:OFF Generated code

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result
                    + ((mimeType == null) ? 0 : mimeType.hashCode());
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
            if (mimeType == null) {
                if (other.mimeType != null)
                    return false;
            } else if (!mimeType.equals(other.mimeType))
                return false;
            return true;
        }

        // CHECKSTYLE:ON

    }

}
