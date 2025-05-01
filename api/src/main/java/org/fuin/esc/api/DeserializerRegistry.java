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

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * Locates a deserializer for a given type, version and encoding combination.
 */
public interface DeserializerRegistry {

    /**
     * Tries to find a deserializer for the given combination.
     *
     * @param type     Unique identifier for the type of data.
     * @param mimeType Mime type.
     * @return Deserializer instance configured with the arguments or throws an
     * {@link IllegalArgumentException} if no deserializer was found for the type.
     */
    @NotNull
    Deserializer getDeserializer(@NotNull SerializedDataType type, @NotNull EnhancedMimeType mimeType);

    /**
     * Tries to find a deserializer for the given type using the {@link #getDefaultMimeType()}.
     *
     * @param type Unique identifier for the type of data.
     * @return Deserializer instance configured with the arguments oor throws an
     * {@link IllegalArgumentException} if no deserializer was found for the type.
     */
    @NotNull
    Deserializer getDeserializer(@NotNull SerializedDataType type);

    /**
     * Returns the default mime type.
     *
     * @return Default mime type.
     */
    @Nullable
    EnhancedMimeType getDefaultMimeType();

    /**
     * Tries to find a deserializer for the given type using the {@link #getDefaultMimeType()}.
     *
     * @param type Unique identifier for the type of data.
     * @return TRUE if a deserializer was found.
     */
    boolean deserializerExists(@NotNull SerializedDataType type);

    /**
     * Tries to find a deserializer for the given combination.
     *
     * @param type     Unique identifier for the type of data.
     * @param mimeType Mime type.
     * @return TRUE if a deserializer was found.
     */
    boolean deserializerExists(@NotNull SerializedDataType type, @NotNull EnhancedMimeType mimeType);

    /**
     * Defines a builder for the registry.
     *
     * @param <T> Type of the registry.
     * @param <B> Type of the builder.
     */
    interface Builder<T extends DeserializerRegistry, B extends Builder<T, B>> {

        /**
         * Adds a new deserializer to the registry.
         *
         * @param type         Type of the data.
         * @param deserializer Deserializer.
         * @param mimeType     Mime type. In case it's {@literal null}, the base type of the registry will be used.
         */
        B add(@NotNull final SerializedDataType type,
              @NotNull final Deserializer deserializer,
              final EnhancedMimeType mimeType);

        /**
         * Adds a new deserializer to the registry.
         * The base content type of the registry will be used.
         *
         * @param type         Type of the data.
         * @param deserializer Deserializer.
         */
        B add(@NotNull final SerializedDataType type,
              @NotNull final Deserializer deserializer);

        /**
         * Builds an instance of the registry.
         *
         * @return New instance.
         */
        T build();

    }

}
