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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.DeserializerRegistryRequired;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerDeserializer;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistryRequired;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.api.SerializerRegistryRequired;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Serializes and deserializes an object from/to JSON using JSON-B. The content type for serialization is always "application/json". This
 * implementation supports only <code>byte[]</code> for unmarshalling content. Trying to use something else will result in an exception.
 */
@TestOmitted("Test implicitly with other tests")
public final class JacksonDeSerializer implements SerDeserializer, Closeable {

    private final EnhancedMimeType mimeType;

    private final ObjectMapper objectMapper;

    private final List<JsonSerializer<?>> serializers;

    private final List<JsonDeserializer<?>> deserializers;

    private SerializedDataTypeRegistry typeRegistry;

    private boolean initialized;

    /**
     * Constructor with Jackson context classes.
     *
     * @param objectMapper  Jackson mapper to use.
     * @param encoding      Encoding to use.
     * @param serializers   List of configured JSON-B serializers.
     * @param deserializers List of configured JSON-B deserializers.
     */
    private JacksonDeSerializer(@NotNull final ObjectMapper objectMapper,
                                @NotNull final Charset encoding,
                                @NotNull final List<JsonSerializer<?>> serializers,
                                @NotNull final List<JsonDeserializer<?>> deserializers) {
        super();
        Objects.requireNonNull(objectMapper, "objectMapper==null");
        Objects.requireNonNull(encoding, "encoding==null");
        Objects.requireNonNull(serializers, "serializers==null");
        Objects.requireNonNull(deserializers, "deserializers==null");

        this.objectMapper = objectMapper;
        this.mimeType = EnhancedMimeType.create("application", "json", encoding);
        this.serializers = serializers;
        this.deserializers = deserializers;
    }

    @Override
    public EnhancedMimeType getMimeType() {
        return mimeType;
    }

    @Override
    public byte[] marshal(@NotNull final Object obj, @NotNull final SerializedDataType type) {
        ensureInitialized();
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
            objectMapper.writeValue(writer, obj);
            return bos.toByteArray();
        } catch (final IOException ex) {
            throw new RuntimeException("Error serializing data", ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unmarshal(@NotNull final Object data, @NotNull final SerializedDataType type, @NotNull final EnhancedMimeType mimeType) {
        ensureInitialized();
        Objects.requireNonNull(data, "data==null");
        Objects.requireNonNull(type, "type==null");
        Objects.requireNonNull(mimeType, "mimeType==null");

        try {
            final Class<?> clasz = typeRegistry.findClass(type);
            if (clasz.isAssignableFrom(data.getClass())) {
                return (T) data;
            }
            if (data instanceof byte[]) {
                final Reader reader = new InputStreamReader(new ByteArrayInputStream((byte[]) data), mimeType.getEncoding());
                return (T) objectMapper.readValue(reader, clasz);
            }
            throw new IllegalArgumentException("Expected data to be of type byte[], but was: " + data.getClass().getName());

        } catch (final IOException ex) {
            throw new RuntimeException("Error de-serializing data", ex);
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
    public ObjectMapper getObjectMapper() {
        ensureInitialized();
        return objectMapper;
    }

    /**
     * Initializes the instance only with type registry.
     *
     * @param typeRegistry Mapping from type name to type class.
     */
    public void init(@NotNull final SerializedDataTypeRegistry typeRegistry) {
        init(typeRegistry, null, null);
    }

    /**
     * Initializes the instance only with type and serializer registry.
     *
     * @param typeRegistry Mapping from type name to type class.
     * @param serRegistry  Mapping from type name to serializers.
     */
    public void init(@NotNull final SerializedDataTypeRegistry typeRegistry, @Nullable final SerializerRegistry serRegistry) {
        init(typeRegistry, null, serRegistry);
    }

    /**
     * Initializes the instance only with type and deserializer registry.
     *
     * @param typeRegistry  Mapping from type name to type class.
     * @param deserRegistry Mapping from type name to deserializers.
     */
    public void init(@NotNull final SerializedDataTypeRegistry typeRegistry, @Nullable final DeserializerRegistry deserRegistry) {
        init(typeRegistry, deserRegistry, null);
    }

    /**
     * Initializes the instance with necessary registries.
     *
     * @param typeRegistry  Mapping from type name to type class.
     * @param deserRegistry Mapping from type name to deserializers.
     * @param serRegistry   Mapping from type name to serializers.
     */
    public void init(@NotNull final SerializedDataTypeRegistry typeRegistry,
                     @Nullable final DeserializerRegistry deserRegistry,
                     @Nullable final SerializerRegistry serRegistry) {
        if (initialized) {
            throw new IllegalStateException("Instance already initialized - Don't call the init methods more than once");
        }
        this.typeRegistry = typeRegistry;
        initDeserializers(typeRegistry, deserRegistry);
        initSerializers(typeRegistry, serRegistry);
        initialized = true;
    }

    private void initSerializers(SerializedDataTypeRegistry typeRegistry, SerializerRegistry serRegistry) {
        for (final JsonSerializer<?> serializer : serializers) {
            if (serializer instanceof SerializerRegistryRequired ser) {
                if (serRegistry == null) {
                    throw new IllegalStateException(
                            "There is at least one serializer that requires a 'SerializerRegistry', but you didn't provide one (serializer="
                                    + serializer.getClass().getName() + ")");
                }
                ser.setRegistry(serRegistry);
            }
            if (serializer instanceof SerializedDataTypeRegistryRequired ser) {
                if (typeRegistry == null) {
                    throw new IllegalStateException(
                            "There is at least one serializer that requires a 'SerializedDataTypeRegistry', but you didn't provide one (serializer="
                                    + serializer.getClass().getName() + ")");
                }
                ser.setRegistry(typeRegistry);
            }
        }
    }

    private void initDeserializers(SerializedDataTypeRegistry typeRegistry, DeserializerRegistry deserRegistry) {
        for (final JsonDeserializer<?> deserializer : deserializers) {
            if (deserializer instanceof DeserializerRegistryRequired des) {
                if (deserRegistry == null) {
                    throw new IllegalStateException(
                            "There is at least one deserializer that requires a 'DeserializerRegistry', but you didn't provide one (deserializer="
                                    + deserializer.getClass().getName() + ")");
                }
                des.setRegistry(deserRegistry);
            }
            if (deserializer instanceof SerializedDataTypeRegistryRequired des) {
                if (typeRegistry == null) {
                    throw new IllegalStateException(
                            "There is at least one deserializer that requires a 'SerializedDataTypeRegistry', but you didn't provide one (deserializer="
                                    + deserializer.getClass().getName() + ")");
                }
                des.setRegistry(typeRegistry);
            }
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Please call one of the the 'init' methods before executing other methods");
        }
    }

    /**
     * Static convenience method to shorten builder construction.
     *
     * @return New builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create {@link JacksonDeSerializer} instances.
     */
    public static class Builder {

        private final ObjectMapper objectMapper;

        private final Set<JsonSerializer<?>> serializerSet;

        private final Set<JsonDeserializer<?>> deserializerSet;

        private Charset encoding = StandardCharsets.UTF_8;

        public Builder() {
            this(new ObjectMapper());
        }

        public Builder(ObjectMapper objectMapper) {
            super();
            this.objectMapper = objectMapper;
            serializerSet = new HashSet<>();
            deserializerSet = new HashSet<>();
        }

        /**
         * Property used to specify whether the serialized JSON data is formatted with line feeds and indentation.
         * <p>
         * Configures value of {@link SerializationFeature#INDENT_OUTPUT}.
         *
         * @return This builder.
         */
        public Builder withIdentOutput() {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            return this;
        }

        /**
         * Property used to specify whether null values should be serialized to JSON document or skipped.
         * <p>
         * Configures value of {@link JsonInclude.Include#NON_NULL} property.
         *
         * @return This builder.
         */
        public Builder withNonNull() {
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            return this;
        }

        /**
         * Property used to specify custom naming strategy.
         *
         * @param propertyNamingStrategy Custom naming strategy which affects serialization and deserialization.
         * @return This builder.
         */
        public Builder withPropertyNamingStrategy(final PropertyNamingStrategy propertyNamingStrategy) {
            objectMapper.setPropertyNamingStrategy(propertyNamingStrategy);
            return this;
        }

        /**
         * Property used to specify custom serializers.
         *
         * @param serializers Custom serializers which affects serialization.
         * @return This builder.
         */
        public Builder withSerializers(final JsonSerializer<?>... serializers) {
            this.serializerSet.addAll(Arrays.asList(serializers));
            return this;
        }

        /**
         * Property used to specify custom deserializers.
         *
         * @param deserializers Custom deserializers which affects deserialization.
         * @return This builder.
         */
        public Builder withDeserializers(final JsonDeserializer<?>... deserializers) {
            this.deserializerSet.addAll(Arrays.asList(deserializers));
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

        private List<JsonSerializer<?>> registerSerializerModule() {
            final SimpleModule module = new SimpleModule(this.getClass().getName() + "CustomSerializers");
            for (final JsonSerializer<?> serializer : serializerSet) {
                module.addSerializer(serializer);
            }
            objectMapper.registerModule(module);
            return new ArrayList<>(serializerSet);
        }

        @SuppressWarnings("unchecked")
        private List<JsonDeserializer<?>> registerDeserializerModule() {
            final SimpleModule module = new SimpleModule(this.getClass().getName() + "CustomDeserializers");
            for (final JsonDeserializer<?> deserializer : deserializerSet) {
                final Class<Object> cls = (Class<Object>) deserializer.handledType();
                module.addDeserializer(cls, deserializer);
            }
            objectMapper.registerModule(module);
            return new ArrayList<>(deserializerSet);
        }

        /**
         * Creates an instance with the configured values.
         * The builder will NOT be cleared.
         *
         * @return New instance.
         */
        public JacksonDeSerializer build() {
            final List<JsonSerializer<?>> serializers = registerSerializerModule();
            final List<JsonDeserializer<?>> deserializers = registerDeserializerModule();
            return new JacksonDeSerializer(objectMapper, encoding, serializers, deserializers);
        }

    }

}
