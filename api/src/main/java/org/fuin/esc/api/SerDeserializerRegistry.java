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

/**
 * Convenience interface that combines both registry types.
 */
public interface SerDeserializerRegistry extends SerializerRegistry, DeserializerRegistry {

    /**
     * Defines a builder for the registry.
     */
    interface Builder<T extends SerDeserializerRegistry, B extends Builder<T, B>> extends SerializerRegistry.Builder<T, B>, DeserializerRegistry.Builder<T, B> {

        /**
         * Convenience method that adds both, a new serializer and deserializer to the registry.
         *
         * @param type            Type of the data.
         * @param serDeserializer Serializer and deserializer.
         * @param mimeType        Mime type. In case it's {@literal null}, the default type of the registry will be used.
         */
        B add(@NotNull final SerializedDataType type,
              @NotNull final SerDeserializer serDeserializer,
              final EnhancedMimeType mimeType);

        /**
         * Convenience method that adds both, a new serializer and deserializer to the registry.
         * The base content type of the registry will be used.
         *
         * @param type            Type of the data.
         * @param serDeserializer Serializer/Deserializer.
         */
        B add(@NotNull final SerializedDataType type,
              @NotNull final SerDeserializer serDeserializer);

        /**
         * Builds an instance of the registry.
         *
         * @return New instance.
         */
        T build();

    }

}
