/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.api;

import jakarta.validation.constraints.NotNull;

/**
 * Locates a serializer for a given type.
 */
public interface SerializerRegistry {

    /**
     * Tries to find a serializer for the given type.
     * 
     * @param type
     *            Unique identifier for the type of data.
     * 
     * @return Serializer instance or throws a {@link IllegalArgumentException} if no serializer was found.
     */
    @NotNull
    public Serializer getSerializer(@NotNull SerializedDataType type);

    /**
     * Tries to find a serializer for the given type.
     * 
     * @param type
     *            Unique identifier for the type of data.
     * 
     * @return TRUE if a serializer was found for the type.
     */
    public boolean serializerExists(@NotNull SerializedDataType type);

}
