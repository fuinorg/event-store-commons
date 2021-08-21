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

/**
 * Locates a class for a given type.
 */
public interface SerializedDataTypeRegistry {

    /**
     * Tries to find a class for the given type.
     * 
     * @param type
     *            Unique identifier for the type of data.
     * 
     * @return Class or throws a {@link IllegalArgumentException} if no class was found for that type.
     */
    @NotNull
    public Class<?> findClass(@NotNull SerializedDataType type);
    
}
