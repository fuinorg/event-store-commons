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

import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerDeserializer;
import org.fuin.esc.api.SerializedDataType;

import java.nio.charset.Charset;

/**
 * Serializes and deserializes a String object. The content type for serialization is always "text/plain".
 */
public final class TextDeSerializer implements SerDeserializer {

    private final EnhancedMimeType mimeType;

    /**
     * Constructor with UTF-8 encoding.
     */
    public TextDeSerializer() {
        this(Charset.forName("utf-8"));
    }

    /**
     * Constructor with type and encoding.
     * 
     * @param encoding
     *            Default encoding to use.
     */
    public TextDeSerializer(final Charset encoding) {
        super();
        this.mimeType = EnhancedMimeType.create("text", "plain", encoding);
    }

    @Override
    public final EnhancedMimeType getMimeType() {
        return mimeType;
    }

    @Override
    public final byte[] marshal(final Object obj, final SerializedDataType type) {
        if (!(obj instanceof String)) {
            throw new IllegalArgumentException("Can only handle instances of type 'String', but not: "
                    + obj.getClass());
        }
        final String str = (String) obj;
        return str.getBytes(mimeType.getEncoding());
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T unmarshal(final Object data, final SerializedDataType type, final EnhancedMimeType mimeType) {

        if (data instanceof byte[]) {
            return (T) new String((byte[]) data, mimeType.getEncoding());
        }
        if (data instanceof String) {
            // Simply return it
            return (T) data;
        }
        throw new IllegalArgumentException(
                "This deserializer only supports input of type 'String' and 'byte[]', but was: " + data);

    }

}
