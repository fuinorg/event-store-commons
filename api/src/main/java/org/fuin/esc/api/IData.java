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
import org.fuin.objects4j.common.ValueObject;

import java.io.Serializable;

/**
 * Helper class that allows sending the data of an event as XML directly to the
 * event store. Represents a block of data in a serialized form. This class
 * might be useful for tests. It's not used in the 'esc-spi' code itself
 */
public interface IData extends IBaseType, ValueObject, Serializable {

    /**
     * Unique XML/JSON root element name of the type.
     */
    String EL_ROOT_NAME = "data";

    /**
     * XML/JSON name of the {@link #getType()} field.
     */
    String EL_TYPE = "type";

    /**
     * XML/JSON name of the {@link #getMimeType()} field.
     */
    String EL_MIME_TYPE = "mime-type";

    /**
     * Returns the unique identifier for the type of data.
     *
     * @return Unique and never changing type name.
     */
    @NotNull
    String getType();

    /**
     * Returns the Internet Media Type that classifies the data.
     *
     * @return Mime type.
     */
    @NotNull
    EnhancedMimeType getMimeType();

    /**
     * Returns the raw data block.
     *
     * @return Raw data.
     */
    @NotNull
    String getContent();

}
