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
 * Serializes an object.
 */
public interface Deserializer {

    /**
     * Converts the given data into an object.
     *
     * @param data
     *            Serialized object. Can have different types like <code>byte[]</code>, <code>Node</code>
     *            (XML), <code>JsonArray</code> or <code>JsonObject</code>. The possible input depends on the
     *            implementation.
     * @param dataType
     *            Unique name of the data type.
     * @param mimeType
     *            Type of the data in the byte array.
     *
     * @return Deserialized object.
     *
     * @param <T>
     *            Type the data is converted into.
     */
    <T> T unmarshal(@NotNull Object data, @NotNull SerializedDataType dataType, @NotNull EnhancedMimeType mimeType);

}
