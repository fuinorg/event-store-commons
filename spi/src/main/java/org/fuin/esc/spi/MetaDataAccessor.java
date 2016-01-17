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

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Nullable;

/**
 * Provides access to the meta data of unknown type.
 * 
 * @param <TYPE>
 *            Type of the meta data.
 */
public interface MetaDataAccessor<TYPE> {

    /**
     * Initializes the accessor with a given object. Clears any existing state
     * in the accessor.
     * 
     * @param obj
     *            Object to use.
     */
    public void init(@Nullable TYPE obj);

    /**
     * Adds a String parameter to the meta data.
     * 
     * @param key
     *            Name.
     * 
     * @return The value for the key.
     */
    @Nullable
    public String getString(@NotNull String key);

    /**
     * Adds a boolean parameter to the meta data.
     * 
     * @param key
     *            Name.
     * 
     * @return The value for the key.
     */
    @Nullable
    public Boolean getBoolean(@NotNull String key);

    /**
     * Adds a integer parameter to the meta data.
     * 
     * @param key
     *            Name.
     * 
     * @return The value for the key.
     */
    @Nullable
    public Integer getInteger(@NotNull String key);

}
