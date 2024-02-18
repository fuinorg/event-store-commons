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
 * A structure that contains the user's metadata and the system's meta information.
 */
public interface IEscMeta extends IBaseType {

    /** Unique name of the type. */
    TypeName TYPE = new TypeName("EscMeta");

    /** Unique name of the serialized type. */
    SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    /** Unique XML/JSON root element name of the type. */
    String EL_ROOT_NAME = "esc-meta";

    /** XML/JSON tag name of the {@link #getDataType()}. */
    String EL_DATA_TYPE = "data-type";

    /** XML/JSON tag name of the {@link #getDataContentType()}. */
    String EL_DATA_CONTENT_TYPE = "data-content-type";

    /** XML/JSON tag name of the {@link #getMetaType()}. */
    String EL_META_TYPE = "meta-type";

    /** XML/JSON tag name of the {@link #getMetaContentType()}. */
    String EL_META_CONTENT_TYPE = "meta-content-type";

    /**
     * Returns the unique name of the data type.
     *
     * @return Data type.
     */
    @NotNull
    String getDataType();

    /**
     * Returns the type of the data.
     *
     * @return Data type.
     */
    @NotNull
    EnhancedMimeType getDataContentType();

    /**
     * Returns the unique name of the metadata type if available.
     *
     * @return Metadata type.
     */
    @Nullable
    String getMetaType();

    /**
     * Returns the type of the metadata if metadata is available.
     *
     * @return Meta type.
     */
    @Nullable
    EnhancedMimeType getMetaContentType();

    /**
     * Returns the metadata object.
     *
     * @return Metadata object.
     */
    @NotNull
    Object getMeta();

}
