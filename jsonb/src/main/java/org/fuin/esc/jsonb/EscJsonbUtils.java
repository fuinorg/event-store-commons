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

import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilities for the JSON-B serialization module.
 */
public final class EscJsonbUtils {

    /**
     * Private utility constructor.
     */
    private EscJsonbUtils() {
        throw new UnsupportedOperationException("Creating instances of a utility class is not allowed.");
    }

    /**
     * Creates all available JSON-B serializers necessary for the ESC implementation.
     *
     * @return New array with serializers.
     */
    public static JsonbSerializer<?>[] createEscJsonbSerializers() {
        return new JsonbSerializer[]{
                new EscEventsJsonbSerializerDeserializer(),
                new EscEventJsonbSerializerDeserializer(),
                new EscMetaJsonbSerializerDeserializer()
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
     * @return New array with deserializers.
     */
    public static JsonbDeserializer<?>[] createEscJsonbDeserializers() {
        return new JsonbDeserializer[]{
                new EscEventsJsonbSerializerDeserializer(),
                new EscEventJsonbSerializerDeserializer(),
                new EscMetaJsonbSerializerDeserializer()
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
