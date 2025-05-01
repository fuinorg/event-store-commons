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
package org.fuin.esc.jsonb;

import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import org.fuin.esc.api.Deserializer;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.Serializer;
import org.fuin.esc.api.SerializerRegistry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Utilities for the JSON-B serialization module.
 */
public final class EscJsonbUtils {

    /**
     * THE standard mime type (including encoding) that is used by this implementation.
     */
    public static final EnhancedMimeType MIME_TYPE = EnhancedMimeType.create("application", "json", StandardCharsets.UTF_8);

    private static final List<JsonbAdapter<?, ?>> ADAPTERS = List.of(
            new EventIdJsonbAdapter(),
            new TypeNameJsonbAdapter()
    );

    /**
     * Private utility constructor.
     */
    private EscJsonbUtils() {
        throw new UnsupportedOperationException("Creating instances of a utility class is not allowed.");
    }

    /**
     * Returns the list of {@link JsonbAdapter} objects defined by the package.
     *
     * @return New instance of the adapter list.
     */
    public static List<JsonbAdapter<?, ?>> getJsonbAdapters() {
        return new ArrayList<>(ADAPTERS);
    }

    /**
     * Returns an array of {@link JsonbAdapter} objects defined by the package.
     *
     * @return Array with adapters.
     */
    public static JsonbAdapter<?, ?>[] getJsonbAdapterArray() {
        return getJsonbAdapters().toArray(new JsonbAdapter[0]);
    }

    /**
     * Adds the standard ESC types to the registry.
     *
     * @param builder Builder to add the standard types to.
     * @param <T>     Type of the registry.
     * @param <B>     Type of the registry builder.
     * @return The builder.
     */
    public static <T extends SerializedDataTypeRegistry, B extends SerializedDataTypeRegistry.Builder<T, B>> B addEscTypes(B builder) {
        builder.add(EscEvents.SER_TYPE, EscEvents.class);
        builder.add(EscEvent.SER_TYPE, EscEvent.class);
        builder.add(EscMeta.SER_TYPE, EscMeta.class);
        builder.add(Base64Data.SER_TYPE, Base64Data.class);
        return builder;
    }


    /**
     * Creates all ESC standard type adapters needed for JSON-B.
     *
     * @return Adapters.
     */
    public static JsonbAdapter<?, ?>[] createEscJsonbAdapters() {
        return ADAPTERS.toArray(new JsonbAdapter[0]);
    }

    /**
     * Adds the standard ESC types to the registry.
     *
     * @param builder         Builder to add the standard types to.
     * @param serDeserializer Serializer/Deserializer to use.
     * @param <T>             Type of the registry.
     * @param <B>             Type of the registry builder.
     * @return The builder.
     */
    public static <T extends SerDeserializerRegistry, B extends SerDeserializerRegistry.Builder<T, B>> B addEscSerDeserializer(B builder, JsonbSerDeserializer serDeserializer) {
        builder.add(EscEvents.SER_TYPE, serDeserializer, serDeserializer.getMimeType());
        builder.add(EscEvent.SER_TYPE, serDeserializer, serDeserializer.getMimeType());
        builder.add(EscMeta.SER_TYPE, serDeserializer, serDeserializer.getMimeType());
        builder.add(Base64Data.SER_TYPE, serDeserializer, serDeserializer.getMimeType());
        return builder;
    }

    /**
     * Creates all available JSON-B serializers necessary for the ESC implementation.
     *
     * @param serializerRegistry   Serializer registry.
     * @param deserializerRegistry Deserializer registry.
     * @return New array with serializers.
     */
    public static JsonbSerializer<?>[] createEscJsonbSerializers(SerializerRegistry serializerRegistry,
                                                                 DeserializerRegistry deserializerRegistry) {
        return new JsonbSerializer[]{
                new Base64DataSerializerDeserializer(),
                new EscEventsJsonbSerializerDeserializer(),
                new EscEventJsonbSerializerDeserializer(serializerRegistry, deserializerRegistry),
                new EscMetaJsonbSerializerDeserializer(serializerRegistry, deserializerRegistry),
        };
    }

    /**
     * Creates all available JSON-B serializers necessary for the ESC implementation.
     *
     * @return New array with serializers.
     */
    public static JsonbSerializer<?>[] joinJsonbSerializers(final JsonbSerializer<?>[] serializersA,
                                                            final JsonbSerializer<?>... serializersB) {
        return joinJsonbSerializerArrays(serializersA, serializersB);
    }

    /**
     * Creates all available JSON-B serializers necessary for the ESC implementation.
     *
     * @return New array with serializers.
     */
    public static JsonbSerializer<?>[] joinJsonbSerializerArrays(final JsonbSerializer<?>[]... serializerArrays) {
        final List<JsonbSerializer<?>> all = joinArrays(serializerArrays);
        return all.toArray(new JsonbSerializer<?>[0]);
    }

    /**
     * Creates all available JSON-B deserializers necessary for the ESC implementation.
     *
     * @param serializerRegistry   Serializer registry.
     * @param deserializerRegistry Deserializer registry.
     * @return New array with deserializers.
     */
    public static JsonbDeserializer<?>[] createEscJsonbDeserializers(SerializerRegistry serializerRegistry,
                                                                     DeserializerRegistry deserializerRegistry) {
        return new JsonbDeserializer[]{
                new Base64DataSerializerDeserializer(),
                new EscEventsJsonbSerializerDeserializer(),
                new EscEventJsonbSerializerDeserializer(serializerRegistry, deserializerRegistry),
                new EscMetaJsonbSerializerDeserializer(serializerRegistry, deserializerRegistry)
        };
    }

    /**
     * Creates all available JSON-B deserializers necessary for the ESC implementation.
     *
     * @return New array with deserializers.
     */
    public static JsonbDeserializer<?>[] joinJsonbDeserializers(final JsonbDeserializer<?>[] deserializersA,
                                                                final JsonbDeserializer<?>... deserializersB) {
        return joinJsonbDeserializerArrays(deserializersA, deserializersB);
    }

    /**
     * Creates all available JSON-B deserializers necessary for the ESC implementation.
     *
     * @return New array with deserializers.
     */
    public static JsonbDeserializer<?>[] joinJsonbDeserializerArrays(final JsonbDeserializer<?>[]... deserializerArrays) {
        final List<JsonbDeserializer<?>> all = joinArrays(deserializerArrays);
        return all.toArray(new JsonbDeserializer<?>[0]);
    }

    /**
     * Serializes an object under a given key in different formats depending on type and mime-type.
     *
     * @param generator          Generator to use for serialization.
     * @param ctx                Context for serialization.
     * @param serializerRegistry Registry with known types.
     * @param serDataType        Type to serialize.
     * @param key                Key to store the data under.
     * @param data               Data to write.
     */
    public static void serialize(final JsonGenerator generator,
                                 final SerializationContext ctx,
                                 final SerializerRegistry serializerRegistry,
                                 final SerializedDataType serDataType,
                                 final String key,
                                 final Object data) {
        final Serializer serializer = serializerRegistry.getSerializer(serDataType);
        if (serializer.getMimeType().matchEncoding(MIME_TYPE)) {
            // Meta is also JSON (with same encoding) - Just let JSON-B do it's magic
            ctx.serialize(key, data, generator);
        } else {
            // Meta is something else (like XML, TEXT, ...) - Store it Base64
            final byte[] bytes = serializer.marshal(data, serDataType);
            final String base64 = Base64.getEncoder().encodeToString(bytes);
            ctx.serialize(key, base64, generator);
        }
    }

    /**
     * Deserializes JSON content depending on type and mime-type.
     *
     * @param content              JSON structure to deserialize.
     * @param dataType             Type contained in the structure.
     * @param dataContentType      Mime type of the content.
     * @param deserializerRegistry Used to find the deserializer to use.
     * @return Deserialized object.
     */
    public static Object deserialize(final JsonValue content,
                                     final SerializedDataType dataType,
                                     final EnhancedMimeType dataContentType,
                                     DeserializerRegistry deserializerRegistry) {

        if (dataContentType.getEncoding() == null) {
            throw new IllegalStateException("Expected 'meta.data-content-type' to be set, but it's null");
        }

        // Currently JSOB-B has no way to deserialize a JSON object
        // See https://github.com/jakartaee/jsonb-api/issues/111
        // The (ugly) workaround is to serialize/deserialize again
        final byte[] bytes;
        if (content.getValueType() == JsonValue.ValueType.STRING) {
            final String base64 = ((JsonString) content).getString();
            bytes = Base64.getDecoder().decode(base64);
        } else if (content.getValueType() == JsonValue.ValueType.OBJECT) {
            bytes = content.toString().getBytes(dataContentType.getEncoding());
        } else {
            throw new IllegalStateException("Unexpected content type '" + content.getValueType() + "': " + content);
        }
        final Deserializer deserializer = deserializerRegistry.getDeserializer(dataType, dataContentType);
        return deserializer.unmarshal(bytes, dataType, dataContentType);

    }

    /**
     * Creates all available JSON-B serializers necessary for the ESC implementation.
     *
     * @return New array with serializers.
     */
    @SafeVarargs
    static <T> List<T> joinArrays(final T[]... arrays) {
        final List<T> all = new ArrayList<>();
        for (final T[] array : arrays) {
            all.addAll(Arrays.asList(array));
        }
        return all;
    }

}
