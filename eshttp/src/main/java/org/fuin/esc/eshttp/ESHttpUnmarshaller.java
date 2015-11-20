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
package org.fuin.esc.eshttp;

import javax.validation.constraints.NotNull;

import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.objects4j.common.Nullable;

/**
 * Unmarshals data after reading it from the event store.
 */
public interface ESHttpUnmarshaller {

    /**
     * Creates an object from the data.
     * 
     * @param registry
     *            Registry with known deserializers.
     * @param dataType
     *            Unique name of the data type.
     * @param mimeType
     *            Mime type of the data.
     * @param data
     *            Data to unmarshal.
     * 
     * @return Object.
     */
    public Object unmarshal(@NotNull DeserializerRegistry registry, @NotNull SerializedDataType dataType,
            @NotNull EnhancedMimeType mimeType, @Nullable Object data);

}
