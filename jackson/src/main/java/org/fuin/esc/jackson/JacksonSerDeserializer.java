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
package org.fuin.esc.jackson;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerDeserializer;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.objects4j.jackson.ImmutableObjectMapper;
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
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Serializes and deserializes an object from/to JSON using JSON-B. The content type for serialization is always "application/json". This
 * implementation supports only <code>byte[]</code> for unmarshalling content. Trying to use something else will result in an exception.
 */
@TestOmitted("Test implicitly with other tests")
public final class JacksonSerDeserializer implements SerDeserializer, Closeable {

    private final EnhancedMimeType mimeType;

    private final ImmutableObjectMapper.Provider mapperProvider;

    private final SerializedDataTypeRegistry typeRegistry;

    /**
     * Constructor with Jackson context classes.
     *
     * @param mapperProvider Provides the Jackson mapper.
     * @param encoding       Encoding to use.
     * @param typeRegistry   Mapping from type to classes.
     */
    private JacksonSerDeserializer(@NotNull final ImmutableObjectMapper.Provider mapperProvider,
                                   @NotNull final SerializedDataTypeRegistry typeRegistry,
                                   @NotNull final Charset encoding) {
        super();
        Objects.requireNonNull(mapperProvider, "mapperProvider==null");
        Objects.requireNonNull(typeRegistry, "typeRegistry==null");
        Objects.requireNonNull(encoding, "encoding==null");

        this.mapperProvider = mapperProvider;
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
            mapperProvider.writer().writeValue(writer, obj);
            return bos.toByteArray();
        } catch (final IOException ex) {
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
            if (data instanceof byte[]) {
                final Reader reader = new InputStreamReader(new ByteArrayInputStream((byte[]) data), mimeType.getEncoding());
                return (T) mapperProvider.reader().readValue(reader, clasz);
            }
            throw new IllegalArgumentException("Expected data to be of type byte[], but was: " + data.getClass().getName());

        } catch (final IOException ex) {
            final String dataStr = new String((byte[]) data, mimeType.getEncoding());
            throw new RuntimeException("Error de-serializing data of type '" + type + "': " + dataStr, ex);
        }
    }

    @Override
    public void close() throws IOException {
        // Do nothing
    }

    /**
     * Returns the JSON-B instance for direct use.
     *
     * @return Correctly configured instance.
     */
    @NotNull
    public ImmutableObjectMapper mapper() {
        return mapperProvider.mapper();
    }

    /**
     * Convenience method for static access.
     *
     * @return New builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create {@link JacksonSerDeserializer} instances.
     */
    public static class Builder {

        private ImmutableObjectMapper.Provider mapperProvider;

        private SerializedDataTypeRegistry typeRegistry;

        private Charset encoding = StandardCharsets.UTF_8;

        public Builder() {
            super();
        }

        /**
         * Sets the object mapper provider.
         *
         * @param mapperProvider Object mapper provider.
         * @return This builder.
         */
        public Builder withObjectMapper(final ImmutableObjectMapper.Provider mapperProvider) {
            this.mapperProvider = mapperProvider;
            return this;
        }

        /**
         * Sets the type registry.
         *
         * @param typeRegistry Registry that has the type to class mapping.
         * @return This builder.
         */
        public Builder withTypeRegistry(final SerializedDataTypeRegistry typeRegistry) {
            this.typeRegistry = typeRegistry;
            return this;
        }

        /**
         * The binding operations will default to this property for encoding of JSON data. For input data (fromJson), selected encoding is
         * used if the encoding cannot be detected automatically. Default value is 'UTF-8'.
         *
         * @param encoding Valid character encoding as defined in the <a href="http://tools.ietf.org/html/rfc7159">RFC 7159</a> and supported by
         *                 Java Platform.
         * @return This builder.
         */
        public Builder withEncoding(final Charset encoding) {
            this.encoding = encoding;
            return this;
        }

        /**
         * Creates an instance with the configured values.
         * The builder will NOT be cleared.
         *
         * @return New instance.
         */
        public JacksonSerDeserializer build() {
            return new JacksonSerDeserializer(mapperProvider, typeRegistry, encoding);
        }

    }

}
