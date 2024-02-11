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
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Immutable;
import org.fuin.objects4j.common.ValueObject;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents a block of data in a serialized form.
 */
@Immutable
public class SerializedData implements ValueObject, Serializable {

    private static final long serialVersionUID = 1000L;

    /** Unique type of the data. */
    @NotNull
    private String type;

    /** Internet Media Type that classifies the raw event data. */
    @NotNull
    private String mimeType;

    /** Raw event data in format defined by the mime type and encoding. */
    @NotNull
    private byte[] raw;

    /**
     * Protected constructor for deserialization.
     */
    protected SerializedData() { //NOSONAR Ignore uninitialized fields
        super();
    }

    /**
     * Creates a data object.
     * 
     * @param type
     *            Unique identifier for the type of data.
     * @param mimeType
     *            Internet Media Type that classifies the data.
     * @param raw
     *            Raw data block.
     */
    public SerializedData(@NotNull final SerializedDataType type,
                          @NotNull final EnhancedMimeType mimeType, @NotNull final byte[] raw) {
        super();

        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("mimeType", mimeType);
        Contract.requireArgNotNull("raw", raw);

        this.type = type.asBaseType();
        this.mimeType = mimeType.toString();
        this.raw = raw;
    }

    /**
     * Returns the unique identifier for the type of data.
     * 
     * @return Unique and never changing type name.
     */
    @NotNull
    public final SerializedDataType getType() {
        return new SerializedDataType(type);
    }

    /**
     * Returns the Internet Media Type that classifies the data.
     * 
     * @return Mime type.
     */
    @NotNull
    public final EnhancedMimeType getMimeType() {
        return EnhancedMimeType.create(mimeType);
    }

    /**
     * Returns the raw data block.
     * 
     * @return Raw data.
     */
    @NotNull
    public final byte[] getRaw() {
        return raw;
    }

    // CHECKSTYLE:OFF Generated code
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((mimeType == null) ? 0 : mimeType.hashCode());
        result = prime * result + Arrays.hashCode(raw);
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SerializedData))
            return false;
        SerializedData other = (SerializedData) obj;
        if (mimeType == null) {
            if (other.mimeType != null)
                return false;
        } else if (!mimeType.equals(other.mimeType))
            return false;
        if (!Arrays.equals(raw, other.raw))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    // CHECKSTYLE:ON

}
