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

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * An event structure.
 */
public interface IEscEvent extends IBaseType {

    /** Unique XML/JSON root element name of the type. */
    String EL_ROOT_NAME = "Event";

    /** XML/JSON name of the {@link #getEventId()} field. */
    String EL_EVENT_ID = "EventId";

    /** XML/JSON name of the {@link #getEventType()} field. */
    String EL_EVENT_TYPE = "EventType";

    /** XML/JSON name of the {@link #getData()} field. */
    String EL_DATA = "Data";

    /** XML/JSON name of the {@link #getMeta()} field. */
    String EL_META_DATA = "MetaData";

    /** Unique name of the type. */
    TypeName TYPE = new TypeName(EL_ROOT_NAME);

    /** Unique name of the serialized type. */
    SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    /**
     * Returns the unique event identifier.
     *
     * @return Event ID.
     */
    @NotNull
    String getEventId();

    /**
     * Returns the unique type name of the event.
     *
     * @return Event type.
     */
    @NotNull
    String getEventType();

    /**
     * Returns the data.
     *
     * @return Data.
     */
    @NotNull
    IDataWrapper getData();

    /**
     * Returns the metadata.
     *
     * @return Metadata.
     */
    @Nullable
    IDataWrapper getMeta();

}
