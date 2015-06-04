/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.spi;

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Nullable;

/**
 * Handles building meta data.
 * 
 * @param <TYPE>
 *            Type of the meta data.
 */
public interface MetaDataBuilder<TYPE> {

    /**
     * Initializes the build with a given object. Clears any existing state in
     * the builder.
     * 
     * @param obj
     *            Object to start with.
     */
    public void init(@Nullable TYPE obj);

    /**
     * Adds a String parameter to the meta data.
     * 
     * @param key
     *            Name.
     * @param value
     *            Value.
     */
    public void add(@NotNull String key, @NotNull String value);

    /**
     * Adds a boolean parameter to the meta data.
     * 
     * @param key
     *            Name.
     * @param value
     *            Value.
     */
    public void add(@NotNull String key, @NotNull boolean value);

    /**
     * Adds a integer parameter to the meta data.
     * 
     * @param key
     *            Name.
     * @param value
     *            Value.
     */
    public void add(@NotNull String key, @NotNull int value);

    /**
     * Prepares the final object.
     * 
     * @return Created object.
     */
    @NotNull
    public TYPE build();

}
