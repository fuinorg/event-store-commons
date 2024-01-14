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

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * Locates a deserializer for a given type, version and encoding combination.
 */
public interface DeserializerRegistry {

    /**
     * Tries to find a deserializer for the given combination.
     * 
     * @param type
     *            Unique identifier for the type of data.
     * @param mimeType
     *            Mime type.
     * 
     * @return Deserializer instance configured with the arguments or throws an
     *         {@link IllegalArgumentException} if no deserializer was found for the type.
     */
    @NotNull
    public Deserializer getDeserializer(@NotNull SerializedDataType type, @NotNull EnhancedMimeType mimeType);

    /**
     * Tries to find a deserializer for the given type using the
     * {@link #getDefaultContentType(SerializedDataType)}. This method fails if the default mime type is not
     * set.
     * 
     * @param type
     *            Unique identifier for the type of data.
     * 
     * @return Deserializer instance configured with the arguments oor throws an
     *         {@link IllegalArgumentException} if no deserializer was found for the type.
     */
    @NotNull
    public Deserializer getDeserializer(@NotNull SerializedDataType type);

    /**
     * Returns the default mime type for the given type.
     * 
     * @param type
     *            Unique identifier for the type of data.
     * 
     * @return Default mime type or NULL if nothing was configured for the given type.
     */
    @Nullable
    public EnhancedMimeType getDefaultContentType(@NotNull SerializedDataType type);

    /**
     * Tries to find a deserializer for the given type using the
     * {@link #getDefaultContentType(SerializedDataType)}. This method fails if the default mime type is not
     * set.
     * 
     * @param type
     *            Unique identifier for the type of data.
     * 
     * @return TRUE if a deserializer was found.
     */
    public boolean deserializerExists(@NotNull SerializedDataType type);

    /**
     * Tries to find a deserializer for the given combination.
     * 
     * @param type
     *            Unique identifier for the type of data.
     * @param mimeType
     *            Mime type.
     * 
     * @return TRUE if a deserializer was found.
     */
    public boolean deserializerExists(@NotNull SerializedDataType type, @NotNull EnhancedMimeType mimeType);

}
