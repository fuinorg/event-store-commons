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
package org.fuin.esc.jsonb;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IData;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ValueObject;

import javax.annotation.concurrent.Immutable;
import java.io.Serial;
import java.io.Serializable;

/**
 * Helper class that allows sending the data of an event as XML directly to the
 * event store. Represents a block of data in a serialized form. This class
 * might be useful for tests. It's not used in the 'esc-spi' code itself
 */
@Immutable
public final class Data implements IData, ValueObject, Serializable {

    @Serial
    private static final long serialVersionUID = 1000L;

    /**
     * Unique type of the data.
     */
    @NotNull
    private String type;

    /**
     * Internet Media Type that classifies the raw event data.
     */
    @NotNull
    private String mimeType;

    /**
     * Raw event data in format defined by the mime type and encoding.
     */
    @NotNull
    private String content;

    /**
     * Protected constructor for deserialization.
     */
    protected Data() { //NOSONAR Ignore uninitialized fields
        super();
    }

    /**
     * Creates a data object.
     *
     * @param type     Unique identifier for the type of data.
     * @param mimeType Internet Media Type with encoding and version that classifies
     *                 the data.
     * @param content  Content.
     */
    public Data(@NotNull final String type,
                @NotNull final EnhancedMimeType mimeType,
                @NotNull final String content) {
        super();

        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("mimeType", mimeType);
        Contract.requireArgNotNull("content", content);

        this.type = type;
        this.mimeType = mimeType.toString();
        this.content = content;

    }

    /**
     * Returns the unique identifier for the type of data.
     *
     * @return Unique and never changing type name.
     */
    @NotNull
    public String getType() {
        return type;
    }

    /**
     * Returns the Internet Media Type that classifies the data.
     *
     * @return Mime type.
     */
    @NotNull
    public EnhancedMimeType getMimeType() {
        return EnhancedMimeType.create(mimeType);
    }

    /**
     * Returns the raw data block.
     *
     * @return Raw data.
     */
    @NotNull
    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Data{" +
                "type='" + type + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", content='" + content + '\'' +
                '}';
    }

}
