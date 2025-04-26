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
package org.fuin.esc.api;

import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains all known serializers and deserializers.
 */
public final class SimpleSerializerDeserializerRegistry implements SerDeserializerRegistry {

    private final EnhancedMimeType defaultMimeType;

    private final Map<SerializedDataType, Serializer> serMap;

    private final Map<Key, Deserializer> desMap;

    /**
     * Default constructor.
     */
    private SimpleSerializerDeserializerRegistry(final EnhancedMimeType defaultMimeType) {
        super();
        serMap = new HashMap<>();
        desMap = new HashMap<>();
        this.defaultMimeType = Objects.requireNonNull(defaultMimeType, "defaultMimeType==null");
    }

    private void addSerDeserializer(@NotNull final SerializedDataType type,
                                    @NotNull final SerDeserializer serDeserializer,
                                    final EnhancedMimeType mimeType) {
        this.addSerializer(type, serDeserializer);
        this.addDeserializer(type, serDeserializer, mimeType);
    }

    private void addDeserializer(@NotNull final SerializedDataType type,
                                 @NotNull final Deserializer deserializer,
                                 final EnhancedMimeType mimeType) {
        final Key key = new Key(type, mimeType == null ? defaultMimeType : mimeType);
        desMap.put(key, deserializer);
    }

    private void addSerializer(@NotNull final SerializedDataType type,
                               @NotNull final Serializer serializer) {
        serMap.put(type, serializer);
    }

    @Override
    public Serializer getSerializer(final SerializedDataType type) {
        Contract.requireArgNotNull("type", type);
        final Serializer ser = serMap.get(type);
        if (ser == null) {
            throw new IllegalArgumentException("No serializer found for: " + type);
        }
        return ser;
    }

    @Override
    public Deserializer getDeserializer(final SerializedDataType type,
                                        final EnhancedMimeType mimeType) {

        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("mimeType", mimeType);

        final Key key = new Key(type, mimeType);
        final Deserializer des = desMap.get(key);
        if (des == null) {
            throw new IllegalArgumentException("No deserializer found for: " + key);
        }
        return des;

    }

    @Override
    public Deserializer getDeserializer(final SerializedDataType type) {
        Contract.requireArgNotNull("type", type);

        final Key key = new Key(type, defaultMimeType);
        final Deserializer des = desMap.get(key);
        if (des == null) {
            throw new IllegalArgumentException("No deserializer found for: " + key);
        }
        return des;
    }

    @Override
    public EnhancedMimeType getDefaultMimeType() {
        return defaultMimeType;
    }

    @Override
    public boolean serializerExists(final SerializedDataType type) {
        final Serializer ser = serMap.get(type);
        return ser != null;
    }

    @Override
    public boolean deserializerExists(final SerializedDataType type) {
        Contract.requireArgNotNull("type", type);

        final Key key = new Key(type, defaultMimeType);
        final Deserializer des = desMap.get(key);
        return des != null;
    }

    @Override
    public boolean deserializerExists(final SerializedDataType type, final EnhancedMimeType mimeType) {
        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("mimeType", mimeType);

        final Key key = new Key(type, mimeType);
        final Deserializer des = desMap.get(key);
        return des != null;
    }

    /**
     * Key used to find an appropriate deserializer.
     */
    private static class Key {

        private final SerializedDataType type;
        private final EnhancedMimeType mimeType;

        public Key(final SerializedDataType type, final EnhancedMimeType mimeType) {
            this.type = Objects.requireNonNull(type, "type ==null");
            Objects.requireNonNull(mimeType, "mimeType ==null");
            this.mimeType = EnhancedMimeType.create(mimeType.getPrimaryType(), mimeType.getSubType(), mimeType.getEncoding(), mimeType.getVersion());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(type, key.type) && Objects.equals(mimeType, key.mimeType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, mimeType);
        }

        @Override
        public String toString() {
            return "Key [type=" + type + ", mimeType=" + mimeType + "]";
        }

    }

    /**
     * Builds an instance of the outer class.
     */
    public static final class Builder implements SerDeserializerRegistry.Builder<SimpleSerializerDeserializerRegistry, Builder> {

        private SimpleSerializerDeserializerRegistry delegate;

        public Builder(EnhancedMimeType defaultMimeType) {
            delegate = new SimpleSerializerDeserializerRegistry(defaultMimeType);
        }

        @Override
        public Builder add(SerializedDataType type, SerDeserializer serDeserializer, EnhancedMimeType mimeType) {
            Objects.requireNonNull(type, "type==null");
            Objects.requireNonNull(serDeserializer, "serDeserializer==null");

            delegate.addSerDeserializer(type, serDeserializer, mimeType);
            return this;
        }

        @Override
        public Builder add(SerializedDataType type, SerDeserializer serDeserializer) {
            Objects.requireNonNull(type, "type==null");
            Objects.requireNonNull(serDeserializer, "serDeserializer==null");

            delegate.addSerDeserializer(type, serDeserializer, null);
            return this;
        }

        @Override
        public Builder add(SerializedDataType type, Deserializer deserializer, EnhancedMimeType mimeType) {
            Objects.requireNonNull(type, "type==null");
            Objects.requireNonNull(deserializer, "deserializer==null");

            delegate.addDeserializer(type, deserializer, mimeType);
            return this;
        }

        @Override
        public Builder add(SerializedDataType type, Deserializer deserializer) {
            Objects.requireNonNull(type, "type==null");
            Objects.requireNonNull(deserializer, "deserializer==null");

            delegate.addDeserializer(type, deserializer, null);
            return this;
        }

        @Override
        public Builder add(SerializedDataType type, Serializer serializer) {
            Objects.requireNonNull(type, "type==null");
            Objects.requireNonNull(serializer, "serializer==null");

            delegate.addSerializer(type, serializer);
            return this;
        }

        public SimpleSerializerDeserializerRegistry build() {
            final SimpleSerializerDeserializerRegistry tmp = delegate;
            delegate = new SimpleSerializerDeserializerRegistry(tmp.getDefaultMimeType());
            return tmp;
        }

    }

}
