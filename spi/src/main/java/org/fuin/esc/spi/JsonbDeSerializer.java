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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.JsonbException;

/**
 * Serializes and deserializes an object from/to JSON using JSON-B. The content type for serialization is always "application/json". This
 * implementation supports only <code>byte[]</code> for marshalling/unmarshalling content. Trying to unmarshal anything else will simply
 * return the input without any change.
 */
public final class JsonbDeSerializer implements SerDeserializer {

    private final EnhancedMimeType mimeType;

    private final Jsonb jsonb;

    private final SerializedDataTypeRegistry registry;

    /**
     * Constructor that creates a JAXB context internally and uses UTF-8 encoding.
     * 
     * @param config
     *            JSON-B configuration to use.
     * @param registry
     *            Registry with types to serialize/deserialize with JSON-B.
     */
    public JsonbDeSerializer(final JsonbConfig config, final SerializedDataTypeRegistry registry) {
        this(config, Charset.forName("utf-8"), registry);
    }

    /**
     * Constructor with JAXB context classes.
     * 
     * @param config
     *            JSON-B configuration to use.
     * @param encoding
     *            Encoding to use.
     * @param registry
     *            Registry with types to serialize/deserialize with JSON-B.
     */
    public JsonbDeSerializer(final JsonbConfig config, final Charset encoding, final SerializedDataTypeRegistry registry) {
        super();
        this.jsonb = JsonbBuilder.create(config);
        this.mimeType = EnhancedMimeType.create("application", "json", encoding);
        this.registry = registry;
    }

    @Override
    public final EnhancedMimeType getMimeType() {
        return mimeType;
    }

    @Override
    public final byte[] marshal(final Object obj, final SerializedDataType type) {

        try {
            final Class<?> clasz = registry.findClass(type);
            if (!obj.getClass().isAssignableFrom(clasz)) {
                throw new IllegalStateException("The instance class '" + obj.getClass().getName() + "' is not assignable from '"
                        + clasz.getName() + "'. The registry returned an incompatible type for '" + type + "'");
            }
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            final Writer writer = new OutputStreamWriter(bos, mimeType.getEncoding());
            jsonb.toJson(obj, writer);
            return bos.toByteArray();
        } catch (final JsonbException ex) {
            throw new RuntimeException("Error serializing data", ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T unmarshal(final Object data, final SerializedDataType type, final EnhancedMimeType mimeType) {
        try {

            if (data instanceof byte[]) {
                final Class<?> clasz = registry.findClass(type);
                final Reader reader = new InputStreamReader(new ByteArrayInputStream((byte[]) data), mimeType.getEncoding());
                return (T) jsonb.fromJson(reader, clasz);
            }
            return (T) data;

        } catch (final JsonbException ex) {
            throw new RuntimeException("Error de-serializing data", ex);
        }
    }

}
