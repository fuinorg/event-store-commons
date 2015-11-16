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
package org.fuin.esc.spi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonWriter;

/**
 * Serializes and deserializes a JSON object. The content type for serialization is always "application/json".
 * This implementation supports {@link JsonStructure} and
 * <code>byte[]</data> for unmarshalling content. Type {@link JsonStructure} will simply return the input without any change.
 */
public final class JsonDeSerializer implements SerDeserializer {

    private final EnhancedMimeType mimeType;

    /**
     * Constructor with UTF-8 encoding.
     */
    public JsonDeSerializer() {
        this(Charset.forName("utf-8"));
    }

    /**
     * Constructor with type and encoding.
     * 
     * @param encoding
     *            Default encoding to use.
     */
    public JsonDeSerializer(final Charset encoding) {
        super();
        this.mimeType = EnhancedMimeType.create("application", "json", encoding);
    }

    @Override
    public final EnhancedMimeType getMimeType() {
        return mimeType;
    }

    @Override
    public final byte[] marshal(final Object obj) {
        if (!(obj instanceof JsonStructure)) {
            throw new IllegalArgumentException("Can only handle instances of type 'JsonStructure', but not: "
                    + obj.getClass());
        }
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        final Writer writer = new OutputStreamWriter(bos, mimeType.getEncoding());
        final JsonWriter jsonWriter = Json.createWriter(writer);
        try {
            jsonWriter.write((JsonStructure) obj);
        } finally {
            jsonWriter.close();
        }
        return bos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T unmarshal(final Object data, final EnhancedMimeType mimeType) {

        if (data instanceof byte[]) {
            final Reader reader = new InputStreamReader(new ByteArrayInputStream((byte[]) data),
                    mimeType.getEncoding());
            final JsonReader jsonReader = Json.createReader(reader);
            try {
                return (T) jsonReader.read();
            } finally {
                jsonReader.close();
            }
        }
        if (data instanceof JsonStructure) {
            // Simply return it
            return (T) data;
        }
        throw new IllegalArgumentException("This deserializer only supports input of type '"
                + JsonStructure.class.getName() + "' and 'byte[]', but was: " + data);

    }

}
