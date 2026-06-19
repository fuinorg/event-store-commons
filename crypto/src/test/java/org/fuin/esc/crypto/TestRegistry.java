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
package org.fuin.esc.crypto;

import org.fuin.esc.api.Deserializer;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.Serializer;
import org.fuin.esc.api.SerializerRegistry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Test registry that serializes any object using plain Java serialization. Acts as both serializer and deserializer registry.
 */
public final class TestRegistry implements SerializerRegistry, DeserializerRegistry, Serializer, Deserializer {

    private final EnhancedMimeType mimeType = EnhancedMimeType.create("application", "octet-stream", StandardCharsets.UTF_8);

    @Override
    public Serializer getSerializer(final SerializedDataType type) {
        return this;
    }

    @Override
    public boolean serializerExists(final SerializedDataType type) {
        return true;
    }

    @Override
    public Deserializer getDeserializer(final SerializedDataType type, final EnhancedMimeType mimeType) {
        return this;
    }

    @Override
    public Deserializer getDeserializer(final SerializedDataType type) {
        return this;
    }

    @Override
    public EnhancedMimeType getDefaultMimeType() {
        return mimeType;
    }

    @Override
    public boolean deserializerExists(final SerializedDataType type) {
        return true;
    }

    @Override
    public boolean deserializerExists(final SerializedDataType type, final EnhancedMimeType mimeType) {
        return true;
    }

    @Override
    public EnhancedMimeType getMimeType() {
        return mimeType;
    }

    @Override
    public <T> byte[] marshal(final T obj, final SerializedDataType type) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(obj);
            }
            return bos.toByteArray();
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to marshal", ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unmarshal(final Object data, final SerializedDataType dataType, final EnhancedMimeType mimeType) {
        try {
            final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream((byte[]) data));
            return (T) ois.readObject();
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to unmarshal", ex);
        }
    }

}
