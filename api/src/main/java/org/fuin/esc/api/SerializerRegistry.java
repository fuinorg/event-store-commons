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
import org.fuin.objects4j.common.ThreadSafe;

/**
 * Locates a serializer for a given type.
 * <p>
 * Implementors are expected to be thread-safe.
 */
@ThreadSafe
public interface SerializerRegistry {

    /**
     * Tries to find a serializer for the given type.
     *
     * @param type Unique identifier for the type of data.
     * @return Serializer instance or throws a {@link IllegalArgumentException} if no serializer was found.
     */
    @NotNull
    Serializer getSerializer(SerializedDataType type);

    /**
     * Tries to find a serializer for the given type.
     *
     * @param type Unique identifier for the type of data.
     * @return TRUE if a serializer was found for the type.
     */
    boolean serializerExists(SerializedDataType type);

    /**
     * Defines a builder for the registry.
     *
     * @param <T> Type of the registry.
     * @param <B> Type of the builder.
     */
    interface Builder<T extends SerializerRegistry, B extends SerializerRegistry.Builder<T, B>> {

        /**
         * Adds a new serializer to the registry.
         *
         * @param type       Type of the data.
         * @param serializer Serializer to add.
         */
        B add(final SerializedDataType type, final Serializer serializer);

        /**
         * Builds an instance.
         *
         * @return New instance.
         */
        T build();

    }

}
