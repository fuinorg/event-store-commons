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

import jakarta.annotation.Nullable;
import jakarta.json.JsonStructure;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.config.PropertyNamingStrategy;
import jakarta.json.bind.config.PropertyVisibilityStrategy;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
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
import java.util.List;
import java.util.Locale;

/**
 * Serializes and deserializes an object from/to JSON using JSON-B. The content type for serialization is always "application/json". This
 * implementation supports only <code>byte[]</code> for unmarshalling content. Trying to use something else will result in an exception.
 */
public final class JsonbDeSerializer implements SerDeserializer, Closeable {

    private final EnhancedMimeType mimeType;

    private final Jsonb jsonb;

    private final List<JsonbSerializer<?>> serializers;

    private final List<JsonbDeserializer<?>> deserializers;

    private SerializedDataTypeRegistry typeRegistry;

    private boolean initialized;

    /**
     * Constructor with JAXB context classes.
     *
     * @param config
     *            JSON-B configuration to use.
     * @param encoding
     *            Encoding to use.
     * @param serializers
     *            List of configured JSON-B serializers.
     * @param deserializers
     *            List of configured JSON-B deserializers.
     */
    private JsonbDeSerializer(@NotNull final JsonbConfig config,
                              @NotNull final Charset encoding,
                              @NotNull final List<JsonbSerializer<?>> serializers,
                              @NotNull final List<JsonbDeserializer<?>> deserializers) {
        super();
        this.jsonb = JsonbBuilder.create(config);
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
        try {
            final Class<?> clasz = typeRegistry.findClass(type);
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
    public <T> T unmarshal(@NotNull final Object data, @NotNull final SerializedDataType type, @NotNull final EnhancedMimeType mimeType) {
        ensureInitialized();
        try {
            final Class<?> clasz = typeRegistry.findClass(type);
            if (clasz.isAssignableFrom(data.getClass())) {
                return (T) data;
            }
            if (data instanceof JsonStructure js) {
                return (T) jsonb.fromJson(js.toString(), clasz);
            }
            if (data instanceof byte[]) {
                final Reader reader = new InputStreamReader(new ByteArrayInputStream((byte[]) data), mimeType.getEncoding());
                return (T) jsonb.fromJson(reader, clasz);
            }
            throw new IllegalArgumentException("Expected data to be of type byte[], but was: " + data.getClass().getName());

        } catch (final JsonbException ex) {
            throw new RuntimeException("Error de-serializing data", ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            jsonb.close();
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
    public Jsonb getJsonb() {
        ensureInitialized();
        return jsonb;
    }

    /**
     * Initializes the instance only with type registry.
     *
     * @param typeRegistry
     *            Mapping from type name to type class.
     */
    public void init(@NotNull final SerializedDataTypeRegistry typeRegistry) {
        init(typeRegistry, null, null);
    }

    /**
     * Initializes the instance only with type and serializer registry.
     *
     * @param typeRegistry
     *            Mapping from type name to type class.
     * @param serRegistry
     *            Mapping from type name to serializers.
     */
    public void init(@NotNull final SerializedDataTypeRegistry typeRegistry, @Nullable final SerializerRegistry serRegistry) {
        init(typeRegistry, null, serRegistry);
    }

    /**
     * Initializes the instance only with type and deserializer registry.
     *
     * @param typeRegistry
     *            Mapping from type name to type class.
     * @param deserRegistry
     *            Mapping from type name to deserializers.
     */
    public void init(@NotNull final SerializedDataTypeRegistry typeRegistry, @Nullable final DeserializerRegistry deserRegistry) {
        init(typeRegistry, deserRegistry, null);
    }

    /**
     * Initializes the instance with necessary registries.
     *
     * @param typeRegistry
     *            Mapping from type name to type class.
     * @param deserRegistry
     *            Mapping from type name to deserializers.
     * @param serRegistry
     *            Mapping from type name to serializers.
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
        for (final JsonbSerializer<?> serializer : serializers) {
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
        for (final JsonbDeserializer<?> deserializer : deserializers) {
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
     * Builder used to create {@link JsonbDeSerializer} instances.
     */
    public static class Builder {

        private final JsonbConfig config;

        private final List<JsonbSerializer<?>> serializers;

        private final List<JsonbDeserializer<?>> deserializers;

        private Charset encoding;

        public Builder() {
            super();
            config = new JsonbConfig();
            serializers = new ArrayList<>();
            deserializers = new ArrayList<>();
        }

        /**
         * Property used to specify whether the serialized JSON data is formatted with linefeeds and indentation.
         *
         * Configures value of {@code FORMATTING} property.
         *
         * @param formatted
         *            True means serialized data is formatted, false (default) means no formatting.
         *
         * @return This builder.
         */
        public Builder withFormatting(final Boolean formatted) {
            config.withFormatting(formatted);
            return this;
        }

        /**
         * Property used to specify whether null values should be serialized to JSON document or skipped.
         *
         * Configures value of {@code NULL_VALUES} property.
         *
         * @param serializeNullValues
         *            True means that null values will be serialized into JSON document, otherwise they will be effectively skipped.
         *
         * @return This builder.
         */
        public Builder withNullValues(final Boolean serializeNullValues) {
            config.withNullValues(serializeNullValues);
            return this;
        }

        /**
         * The binding operations will default to this property for encoding of JSON data. For input data (fromJson), selected encoding is
         * used if the encoding cannot be detected automatically. Default value is 'UTF-8'.
         *
         * Configures value of {@code ENCODING} property.
         *
         * @param encoding
         *            Valid character encoding as defined in the <a href="http://tools.ietf.org/html/rfc7159">RFC 7159</a> and supported by
         *            Java Platform.
         *
         * @return This builder.
         */
        public Builder withEncoding(final Charset encoding) {
            config.withEncoding(encoding.name());
            this.encoding = encoding;
            return this;
        }

        /**
         * Property used to specify whether strict I-JSON serialization compliance should be enforced.
         *
         * Configures value of {@code STRICT_IJSON} property.
         *
         * @param enabled
         *            True means data is serialized in strict compliance according to RFC 7493.
         *
         * @return This builder.
         */
        public Builder withStrictIJSON(final Boolean enabled) {
            config.withStrictIJSON(enabled);
            return this;
        }

        /**
         * Property used to specify custom naming strategy.
         *
         * Configures value of {@code JSONB_PROPERTY_NAMING_STRATEGY} property.
         *
         * @param propertyNamingStrategy
         *            Custom naming strategy which affects serialization and deserialization.
         *
         * @return This builder.
         */
        public Builder withPropertyNamingStrategy(final PropertyNamingStrategy propertyNamingStrategy) {
            config.withPropertyNamingStrategy(propertyNamingStrategy);
            return this;
        }

        /**
         * Property used to specify custom naming strategy.
         *
         * Configures value of {@code JSONB_PROPERTY_NAMING_STRATEGY} property.
         *
         * @param propertyNamingStrategy
         *            Predefined naming strategy which affects serialization and deserialization.
         *
         * @return This builder.
         */
        public Builder withPropertyNamingStrategy(final String propertyNamingStrategy) {
            config.withPropertyNamingStrategy(propertyNamingStrategy);
            return this;
        }

        /**
         * Property used to specify property order strategy.
         *
         * Configures values of {@code JSONB_PROPERTY_ORDER_STRATEGY} property.
         *
         * @param propertyOrderStrategy
         *            Predefined property order strategy which affects serialization.
         *
         * @return This builder.
         */
        public Builder withPropertyOrderStrategy(final String propertyOrderStrategy) {
            config.withPropertyOrderStrategy(propertyOrderStrategy);
            return this;
        }

        /**
         * Property used to specify custom property visibility strategy.
         *
         * Configures value of {@code PROPERTY_VISIBILITY_STRATEGY} property.
         *
         * @param propertyVisibilityStrategy
         *            Custom property visibility strategy which affects serialization and deserialization.
         *
         * @return This builder.
         */
        public Builder withPropertyVisibilityStrategy(final PropertyVisibilityStrategy propertyVisibilityStrategy) {
            config.withPropertyVisibilityStrategy(propertyVisibilityStrategy);
            return this;
        }

        /**
         * Property used to specify custom mapping adapters.
         *
         * Configures value of {@code ADAPTERS} property.
         *
         * Calling withAdapters more than once will merge the adapters with previous value.
         *
         * @param adapters
         *            Custom mapping adapters which affects serialization and deserialization.
         *
         * @return This builder.
         */
        public Builder withAdapters(final JsonbAdapter<?, ?>... adapters) {
            config.withAdapters(adapters);
            return this;
        }

        /**
         * Property used to specify custom serializers.
         *
         * Configures value of {@code SERIALIZERS} property.
         *
         * Calling withSerializers more than once will merge the serializers with previous value.
         *
         * @param serializers
         *            Custom serializers which affects serialization.
         *
         * @return This builder.
         */
        public Builder withSerializers(final JsonbSerializer<?>... serializers) {
            config.withSerializers(serializers);
            this.serializers.addAll(Arrays.asList(serializers));
            return this;
        }

        /**
         * Property used to specify custom deserializers.
         *
         * Configures value of {@code DESERIALIZERS} property.
         *
         * Calling withDeserializers more than once will merge the deserializers with previous value.
         *
         * @param deserializers
         *            Custom deserializers which affects deserialization.
         *
         * @return This builder.
         */
        public Builder withDeserializers(final JsonbDeserializer<?>... deserializers) {
            config.withDeserializers(deserializers);
            this.deserializers.addAll(Arrays.asList(deserializers));
            return this;
        }

        /**
         * Property used to specify custom binary data strategy.
         *
         * Configures value of {@code BINARY_DATA_STRATEGY} property.
         *
         * @param binaryDataStrategy
         *            Custom binary data strategy which affects serialization and deserialization.
         *
         * @return This builder.
         */
        public Builder withBinaryDataStrategy(final String binaryDataStrategy) {
            config.withBinaryDataStrategy(binaryDataStrategy);
            return this;
        }

        /**
         * Property used to specify custom date format. This format will be used by default for all date classes serialization and
         * deserialization.
         *
         * @param dateFormat
         *            Custom date format as specified in {@link java.time.format.DateTimeFormatter}.
         * @param locale
         *            Locale, default is null.
         *
         * @return This builder.
         */
        public Builder withDateFormat(final String dateFormat, final Locale locale) {
            config.withDateFormat(dateFormat, locale);
            return this;
        }

        /**
         * Creates an instance with the configured values.
         *
         * @return New instance.
         */
        public JsonbDeSerializer build() {
            if (encoding == null) {
                withEncoding(StandardCharsets.UTF_8);
            }
            return new JsonbDeSerializer(config, encoding, serializers, deserializers);
        }

    }

}
