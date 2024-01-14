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
import org.fuin.esc.api.SerializedDataType;

/**
 * Serializes an object.
 */
public interface Serializer {

    /**
     * Returns the mime type used to serialize the object.
     * 
     * @return Content type information.
     */
    public EnhancedMimeType getMimeType();

    /**
     * Converts the given object into a byte representation.
     * 
     * @param obj
     *            Object to serialize.
     * @param type
     *            Type of event.
     * 
     * @return Serialized object.
     * 
     * @param <T>
     *            Type the data is converted into.
     */
    @NotNull
    public <T> byte[] marshal(@NotNull T obj, @NotNull SerializedDataType type);

}
