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

import javax.json.JsonStructure;
import javax.validation.constraints.NotNull;

/**
 * Marks an object that can be converted into a JSON object.
 */
public interface ToJsonCapable {

    /**
     * Returns the instance as JSON structure.
     * 
     * @return JSON representation of the instance.
     */
    @NotNull
    public JsonStructure toJson();

    /**
     * Returns the name of the JSON structure.
     * 
     * @return Root name that may be used as 'key' for the object.
     */
    @NotNull
    public String getRootElementName();

}