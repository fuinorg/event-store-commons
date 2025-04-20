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
package org.fuin.esc.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.fuin.esc.api.Deserializer;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.Serializer;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.utils4j.TestOmitted;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utilities for the Jackson serialization module.
 */
@TestOmitted("Currently no methods worth to test")
public final class EscJacksonUtils {

    /**
     * THE standard mime type (including encoding) that is used by this implementation.
     */
    public static final EnhancedMimeType MIME_TYPE = EnhancedMimeType.create("application", "json", StandardCharsets.UTF_8);

    /**
     * Private utility constructor.
     */
    private EscJacksonUtils() {
        throw new UnsupportedOperationException("Creating instances of a utility class is not allowed.");
    }

    /**
     * Creates all available Jackson serializers necessary for the ESC implementation.
     *
     * @return New array with serializers.
     */
    public static JsonSerializer<?>[] createEscJacksonSerializers() {
        return new JsonSerializer[]{
                new Base64DataJacksonSerializer(),
                new EscEventsJacksonSerializer(),
                new EscEventJacksonSerializer(),
                new EscMetaJacksonSerializer()
        };
    }

    /**
     * Creates all available JSON-B deserializers necessary for the ESC implementation.
     *
     * @return New array with deserializers.
     */
    public static JsonDeserializer<?>[] createEscJacksonDeserializers() {
        return new JsonDeserializer[]{
                new Base64DataJacksonDeserializer(),
                new EscEventsJacksonDeserializer(),
                new EscEventJacksonDeserializer(),
                new EscMetaJacksonDeserializer()
        };
    }

    /**
     * Serializes an object under a given key in different formats depending on type and mime-type.
     *
     * @param generator          Generator to use for serialization.
     * @param serializerRegistry Registry with known types.
     * @param serDataType        Type to serialize.
     * @param key                Key to store the data under.
     * @param data               Data to write.
     */
    public static void serialize(final JsonGenerator generator,
                                 final SerializerRegistry serializerRegistry,
                                 final SerializedDataType serDataType,
                                 final String key,
                                 final Object data) {
        try {
            final Serializer serializer = serializerRegistry.getSerializer(serDataType);
            if (serializer.getMimeType().matchEncoding(MIME_TYPE)) {
                // Meta is also JSON (with same encoding) - Just let JSON-B do it's magic
                generator.writeObjectField(key, data);
            } else {
                // Meta is something else (like XML, TEXT, ...) - Store it Base64
                final byte[] bytes = serializer.marshal(data, serDataType);
                final String base64 = Base64.getEncoder().encodeToString(bytes);
                generator.writeStringField(key, base64);
            }
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to serialize '" + key + "': " + data, ex);
        }
    }

    /**
     * Deserializes JSON content depending on type and mime-type.
     *
     * @param dataNode             JSON structure to deserialize.
     * @param dataType             Type contained in the structure.
     * @param dataContentType      Mime type of the content.
     * @param deserializerRegistry Used to find the deserializer to use.
     * @return Deserialized object.
     */
    public static Object deserialize(final JsonNode dataNode,
                                     final SerializedDataType dataType,
                                     final EnhancedMimeType dataContentType,
                                     DeserializerRegistry deserializerRegistry) {

        if (dataContentType.getEncoding() == null) {
            throw new IllegalStateException("Expected 'meta.data-content-type' to be set, but it's null");
        }

        byte[] bytes;
        if (dataNode.getNodeType() == JsonNodeType.STRING) {
            final String base64 = dataNode.asText();
            bytes = Base64.getDecoder().decode(base64);
        } else if (dataNode.isObject()) {
            bytes = dataNode.toString().getBytes(dataContentType.getEncoding());
        } else {
            throw new IllegalStateException("Unexpected content type '" + dataNode.getNodeType() + "': " + dataNode);
        }
        final Deserializer deserializer = deserializerRegistry.getDeserializer(dataType, dataContentType);
        return deserializer.unmarshal(bytes, dataType, dataContentType);

    }

}
