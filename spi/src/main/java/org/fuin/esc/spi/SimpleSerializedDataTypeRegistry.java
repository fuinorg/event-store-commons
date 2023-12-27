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
package org.fuin.esc.spi;

import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all known types and the corresponding class.
 */
public final class SimpleSerializedDataTypeRegistry implements SerializedDataTypeRegistry {

    private final Map<SerializedDataType, Class<?>> map;

    /**
     * Default constructor.
     */
    public SimpleSerializedDataTypeRegistry() {
        super();
        map = new HashMap<>();
    }

    /**
     * Adds a new type/class combination to the registry.
     * 
     * @param type
     *            Type of the data.
     * @param clasz
     *            Class for the type.
     */
    public final void add(@NotNull final SerializedDataType type, final Class<?> clasz) {
        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("clasz", clasz);
        map.put(type, clasz);
    }

    @Override
    @NotNull
    public Class<?> findClass(@NotNull final SerializedDataType type) {
        Contract.requireArgNotNull("type", type);
        final Class<?> clasz = map.get(type);
        if (clasz == null) {
            throw new IllegalArgumentException("No class found for: " + type);
        }
        return clasz;
    }

}
