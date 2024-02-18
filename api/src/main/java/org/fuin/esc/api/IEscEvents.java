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

import java.util.List;

/**
 * A list of events.
 */
public interface IEscEvents extends IBaseType {

    /**
     * Unique root element name of the type.
     */
    String EL_ROOT_NAME = "Events";

    /**
     * XML/JSON name of the elements in the list.
     */
    String EL_EVENT = "Event";

    /**
     * Returns an immutable event list.
     *
     * @return Unmodifiable list of events.
     */
    List<IEscEvent> getList();

}
