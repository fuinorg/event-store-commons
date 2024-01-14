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

import jakarta.json.JsonObject;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.JsonDeSerializer;
import org.fuin.esc.api.SerDeserializer;
import org.fuin.esc.api.SerializedDataType;

import java.nio.charset.Charset;

/**
 * Serializes and deserializes a {@link EscMeta} object as JSON. The content
 * type for serialization is always "application/json".
 */
public class EscMetaJsonDeSerializer implements SerDeserializer {

    private JsonDeSerializer jsonDeSer;

    /**
     * Constructor with UTF-8 encoding.
     */
    public EscMetaJsonDeSerializer() {
        super();
        this.jsonDeSer = new JsonDeSerializer();
    }

    /**
     * Constructor with type and encoding.
     * 
     * @param encoding
     *            Default encoding to use.
     */
    public EscMetaJsonDeSerializer(final Charset encoding) {
        super();
        this.jsonDeSer = new JsonDeSerializer(encoding);
    }

    @Override
    public final EnhancedMimeType getMimeType() {
        return jsonDeSer.getMimeType();
    }

    @Override
    public final <T> byte[] marshal(final T obj, final SerializedDataType type) {
        return jsonDeSer.marshal(obj, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final EscMeta unmarshal(final Object data, final SerializedDataType type, final EnhancedMimeType mimeType) {
        final JsonObject jsonObj = jsonDeSer.unmarshal(data, type, mimeType);
        return EscMeta.create(jsonObj);
    }

}
