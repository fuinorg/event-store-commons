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

/**
 * Converts a type into another one.
 *
 * @param <SOURCE> Source type.
 * @param <TARGET> Target type.
 */
public interface Converter<SOURCE, TARGET> {

    /**
     * Returns the source type.
     *
     * @return Input type.
     */
    Class<SOURCE> getSourceType();

    /**
     * Returns the target type.
     *
     * @return Output type.
     */
    Class<TARGET> getTargetType();

    /**
     * Converts the given source type into the target type.
     *
     * @param source Type to convert.
     *
     * @return Converted type.
     */
    TARGET convert(SOURCE source);

}
