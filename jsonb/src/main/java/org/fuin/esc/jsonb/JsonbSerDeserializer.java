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

import jakarta.json.JsonStructure;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerDeserializer;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.objects4j.jsonb.JsonbProvider;
import org.fuin.utils4j.TestOmitted;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Serializes and deserializes an object from/to JSON using JSON-B. The content type for serialization is always "application/json". This
 * implementation supports only <code>byte[]</code> for unmarshalling content. Trying to use something else will result in an exception.
 */
@TestOmitted("Test implicitly with other tests")
public final class JsonbSerDeserializer implements SerDeserializer, Closeable {

    private final EnhancedMimeType mimeType;

    private final JsonbProvider jsonbProvider;

    private final SerializedDataTypeRegistry typeRegistry;

    /**
     * Constructor with JAXB context classes.
     *
     * @param jsonbProvider Provides the JSON-B implementation to use.
     * @param typeRegistry  Type registry.
     * @param encoding      Encoding to use.
     */
    public JsonbSerDeserializer(@NotNull final JsonbProvider jsonbProvider,
                                @NotNull final SerializedDataTypeRegistry typeRegistry,
                                @NotNull final Charset encoding) {
        super();
        Objects.requireNonNull(jsonbProvider, "jsonbProvider==null");
        Objects.requireNonNull(typeRegistry, "typeRegistry==null");
        Objects.requireNonNull(encoding, "encoding==null");

        this.jsonbProvider = jsonbProvider;
        this.typeRegistry = typeRegistry;
        this.mimeType = EnhancedMimeType.create("application", "json", encoding);
    }

    @Override
    public EnhancedMimeType getMimeType() {
        return mimeType;
    }

    @Override
    public byte[] marshal(@NotNull final Object obj, @NotNull final SerializedDataType type) {
        Objects.requireNonNull(obj, "obj==null");
        Objects.requireNonNull(type, "type==null");
        try {
            final Class<?> clasz = typeRegistry.findClass(type);
            if (!obj.getClass().isAssignableFrom(clasz)) {
                throw new IllegalStateException("The instance class '" + obj.getClass().getName() + "' is not assignable from '"
                        + clasz.getName() + "'. The registry returned an incompatible type for '" + type + "'");
            }
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            final Writer writer = new OutputStreamWriter(bos, mimeType.getEncoding());
            jsonbProvider.jsonb().toJson(obj, writer);
            return bos.toByteArray();
        } catch (final JsonbException ex) {
            throw new RuntimeException("Error serializing data", ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unmarshal(@NotNull final Object data, @NotNull final SerializedDataType type, @NotNull final EnhancedMimeType mimeType) {
        Objects.requireNonNull(data, "data==null");
        Objects.requireNonNull(type, "type==null");
        Objects.requireNonNull(mimeType, "mimeType==null");
        if (!mimeType.getBaseType().equals(this.mimeType.getBaseType())) {
            throw new IllegalArgumentException("Cannot handle: " + mimeType);
        }
        try {
            final Class<?> clasz = typeRegistry.findClass(type);
            if (clasz.isAssignableFrom(data.getClass())) {
                return (T) data;
            }
            if (data instanceof JsonStructure js) {
                return (T) jsonbProvider.jsonb().fromJson(js.toString(), clasz);
            }
            if (data instanceof byte[]) {
                final Reader reader = new InputStreamReader(new ByteArrayInputStream((byte[]) data), mimeType.getEncoding());
                return (T) jsonbProvider.jsonb().fromJson(reader, clasz);
            }
            throw new IllegalArgumentException("Expected data to be of type byte[], but was: " + data.getClass().getName());

        } catch (final JsonbException ex) {
            final String dataStr;
            if (data instanceof JsonStructure js) {
                dataStr = data.toString();
            } else if (data instanceof byte[]) {
                dataStr = new String((byte[])data, mimeType.getEncoding());
            } else {
                dataStr = data.getClass().getName();
            }
            throw new RuntimeException("Error de-serializing data of type '" + type + "': " + dataStr, ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            jsonbProvider.close();
        } catch (final Exception ex) {
            throw new IOException("Failed to clode JSONB instance", ex);
        }
    }

    /**
     * Returns the JSON-B instance for direct use.
     *
     * @return Correctly configured instance.
     */
    @NotNull
    public Jsonb jsonb() {
        return jsonbProvider.jsonb();
    }

}
