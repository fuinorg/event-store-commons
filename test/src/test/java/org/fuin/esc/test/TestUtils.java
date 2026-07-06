package org.fuin.esc.test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.bind.JsonbConfig;
import org.eclipse.yasson.FieldAccessStrategy;
import org.fuin.esc.api.*;
import org.fuin.esc.jackson.EscJacksonModule;
import org.fuin.esc.jackson.EscJacksonUtils;
import org.fuin.esc.jackson.JacksonSerDeserializer;
import org.fuin.esc.jaxb.EscJaxbUtils;
import org.fuin.esc.jaxb.XmlDeSerializer;
import org.fuin.esc.jsonb.EscJsonbUtils;
import org.fuin.esc.jsonb.JsonbSerDeserializer;
import org.fuin.esc.spi.TextDeSerializer;
import org.fuin.objects4j.jackson.ImmutableObjectMapper;
import org.fuin.esc.test.examples.BookAddedEvent;
import org.fuin.esc.test.examples.GreetV1;
import org.fuin.esc.test.examples.GreetV1ToV2Converter;
import org.fuin.esc.test.examples.GreetV2;
import org.fuin.esc.test.examples.MyMeta;
import org.fuin.objects4j.jsonb.JsonbProvider;

import java.nio.charset.StandardCharsets;
import java.util.Objects;


/**
 * Helper methods for the package.
 */
public final class TestUtils {

    public static final String IMPLEMENTATION_KEY = "org.fuin.esc.test.implementation";

    public static final String MEM_IMPLEMENTATION = "mem";

    public static final String JPA_IMPLEMENTATION = "jpa";

    public static final String ESGRPC_IMPLEMENTATION = "esgrpc";

    public static final String ESGRPC_JACKSON_IMPLEMENTATION = "esgrpc-jackson";

    public static final String ESGRPC_ASYNC_IMPLEMENTATION = "esgrpc-async";

    private TestUtils() {
    }

    /**
     * Determines if an object has an expected type in a null-safe way.
     *
     * @param expectedClass Expected type.
     * @param obj           Object to test.
     * @return TRUE if the object is exactly of the same class, else FALSE.
     */
    public static boolean isExpectedType(final Class<?> expectedClass, final Object obj) {
        final Class<?> actualClass;
        if (obj == null) {
            actualClass = null;
        } else {
            actualClass = obj.getClass();
        }
        return Objects.equals(expectedClass, actualClass);
    }

    /**
     * Determines if an exception has an expected type and message in a null-safe way.
     *
     * @param expectedClass   Expected exception type.
     * @param expectedMessage Expected message.
     * @param ex              Exception to test.
     * @return TRUE if the object is exactly of the same class and has the same message, else FALSE.
     */
    public static boolean isExpectedException(final Class<? extends Exception> expectedClass,
                                              final String expectedMessage, final Exception ex) {
        if (!isExpectedType(expectedClass, ex)) {
            return false;
        }
        if ((expectedClass != null) && (expectedMessage != null) && (ex != null)) {
            return Objects.equals(expectedMessage, ex.getMessage());
        }
        return true;
    }

    /**
     * Creates a basic JSON-B configuration.
     *
     * @return Config initialized with some defaults.
     */
    public static JsonbConfig createJsonbConfig() {
        return new JsonbConfig()
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(StandardCharsets.UTF_8.name());
    }

    /**
     * Creates a type registry.
     *
     * @return Type registry.
     */
    public static SerializedDataTypeRegistry createSerializedDataTypeRegistry() {
        return EscJaxbUtils.addEscTypes(new SimpleSerializedDataTypeRegistry.Builder())
                .add(BookAddedEvent.SER_TYPE, BookAddedEvent.class)
                .add(MyMeta.SER_TYPE, MyMeta.class)
                .build();
    }

    /**
     * Creates a pre-configured serializer/deserializer.
     *
     * @param jsonbProvider JSON-B provider.
     * @return New instance.
     */
    public static JsonbSerDeserializer createSerDeserializer(final SerializedDataTypeRegistry typeRegistry, final JsonbProvider jsonbProvider) {
        return new JsonbSerDeserializer(jsonbProvider, typeRegistry, StandardCharsets.UTF_8);
    }

    /**
     * Creates a serializer/deserializer registry with standard and test types.
     *
     * @param xmlDeSer JAX-B serializer/deserializer.
     * @param jsonbDeSer JSON-B serializer/deserializer
     * @param textDeSer Text serializer/deserializer.
     * @return New instance.
     */
    public static SerDeserializerRegistry serDeserializerRegistry(final XmlDeSerializer xmlDeSer,
                                                                  final JsonbSerDeserializer jsonbDeSer,
                                                                  final TextDeSerializer textDeSer) {

        // A "GreetEvent" is stored at version 1 (serializer + v1 deserializer) but understood at version 2 by
        // consumers: register the v2 XmlDeSerializer as a deserializer-ONLY (cast to Deserializer) so it does
        // not overwrite the type-keyed v1 serializer. On read the ConverterRegistry (see converterRegistry())
        // up-casts the deserialized v1 to v2.
        final XmlDeSerializer greetV1 = XmlDeSerializer.builder().add(GreetV1.class).version("1").build();
        final XmlDeSerializer greetV2 = XmlDeSerializer.builder().add(GreetV2.class).version("2").build();

        return EscJaxbUtils.addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(EscJaxbUtils.MIME_TYPE), xmlDeSer)
                .add(new SerializedDataType(BookAddedEvent.TYPE.asBaseType()), xmlDeSer, xmlDeSer.getMimeType())
                .add(new SerializedDataType(MyMeta.TYPE.asBaseType()), jsonbDeSer, jsonbDeSer.getMimeType())
                .add(new SerializedDataType("TextEvent"), textDeSer, textDeSer.getMimeType())
                .add(GreetV1.SER_TYPE, greetV1, greetV1.getMimeType())
                .add(GreetV1.SER_TYPE, (Deserializer) greetV2, greetV2.getMimeType())
                .build();
    }

    /**
     * Creates a fully wired JSON-B serializer/deserializer registry for the KurrentDB (esgrpc) backend. Unlike
     * {@link #serDeserializerRegistry(XmlDeSerializer, JsonbSerDeserializer, TextDeSerializer)} - which stores
     * the event payload and metadata as XML - this registry writes them as JSON, so KurrentDB projections can
     * select events by the categories carried in {@code ev.metadata.categories} (see projection.feature); XML
     * metadata is opaque to the projection engine. The registry brings its own JSON-B config (already
     * registered), so it is self-contained. The Greet up-cast (de)serializers stay XML because the up-cast
     * feature injects the stored version 1 event as raw XML (see upcast-event.feature).
     *
     * @return New JSON-based registry.
     */
    public static SerDeserializerRegistry jsonSerDeserializerRegistry() {

        final SerializedDataTypeRegistry jsonTypeRegistry = EscJsonbUtils
                .addEscTypes(new SimpleSerializedDataTypeRegistry.Builder())
                .add(BookAddedEvent.SER_TYPE, BookAddedEvent.class)
                .add(MyMeta.SER_TYPE, MyMeta.class)
                .build();
        final JsonbConfig jsonbConfig = createJsonbConfig();
        final JsonbProvider jsonbProvider = new JsonbProvider(jsonbConfig);
        final JsonbSerDeserializer jsonbDeSer = createSerDeserializer(jsonTypeRegistry, jsonbProvider);

        final TextDeSerializer textDeSer = new TextDeSerializer();
        final XmlDeSerializer greetV1 = XmlDeSerializer.builder().add(GreetV1.class).version("1").build();
        final XmlDeSerializer greetV2 = XmlDeSerializer.builder().add(GreetV2.class).version("2").build();
        // Some features inject/expect a "BookAddedEvent" as raw application/xml (see read-event.feature); keep
        // an XML deserializer for that content type in addition to the JSON (de)serializer used for writes.
        final XmlDeSerializer bookXml = XmlDeSerializer.builder().add(BookAddedEvent.class).build();

        final SerDeserializerRegistry registry = EscJsonbUtils
                .addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(EscJsonbUtils.MIME_TYPE), jsonbDeSer)
                .add(new SerializedDataType(BookAddedEvent.TYPE.asBaseType()), jsonbDeSer, jsonbDeSer.getMimeType())
                .add(new SerializedDataType(BookAddedEvent.TYPE.asBaseType()), (Deserializer) bookXml, bookXml.getMimeType())
                .add(new SerializedDataType(MyMeta.TYPE.asBaseType()), jsonbDeSer, jsonbDeSer.getMimeType())
                .add(new SerializedDataType("TextEvent"), textDeSer, textDeSer.getMimeType())
                .add(GreetV1.SER_TYPE, greetV1, greetV1.getMimeType())
                .add(GreetV1.SER_TYPE, (Deserializer) greetV2, greetV2.getMimeType())
                .build();

        // Finalize the JSON-B config now that the registry (referenced by the ESC serializers) exists.
        register(jsonbConfig, registry, registry);
        return registry;
    }

    /**
     * Creates a fully wired Jackson serializer/deserializer registry for the KurrentDB (esgrpc) backend. This
     * is the Jackson counterpart of {@link #jsonSerDeserializerRegistry()}: it stores the event payload and
     * metadata as JSON (so KurrentDB projections can select events by {@code ev.metadata.categories}) but uses
     * Jackson instead of JSON-B, letting the TCK exercise the same features against both serializers. The
     * registry brings its own object mapper (already wired), so it is self-contained. The Greet up-cast and the
     * raw XML "BookAddedEvent" (de)serializers stay XML because those features inject raw XML.
     *
     * @return New Jackson-based registry.
     */
    public static SerDeserializerRegistry jacksonSerDeserializerRegistry() {

        final SerializedDataTypeRegistry jacksonTypeRegistry = EscJacksonUtils
                .addEscTypes(new SimpleSerializedDataTypeRegistry.Builder())
                .add(BookAddedEvent.SER_TYPE, BookAddedEvent.class)
                .add(MyMeta.SER_TYPE, MyMeta.class)
                .build();
        // FIELD/ANY visibility mirrors JSON-B's FieldAccessStrategy, so field-only POJOs such as the JAXB
        // annotated BookAddedEvent round-trip through Jackson without needing getters/setters.
        final ImmutableObjectMapper.Builder mapperBuilder = new ImmutableObjectMapper.Builder(
                new ObjectMapper()
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        // Ignore unknown properties like JSON-B does (some features inject a meta with fields
                        // the target type does not declare, e.g. MyMeta gets {"a":"1"} in read-event.feature).
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY));
        final ImmutableObjectMapper.Provider mapperProvider = new ImmutableObjectMapper.Provider(mapperBuilder);
        final JacksonSerDeserializer jacksonDeSer = new JacksonSerDeserializer.Builder()
                .withObjectMapper(mapperProvider)
                .withTypeRegistry(jacksonTypeRegistry)
                .withEncoding(StandardCharsets.UTF_8)
                .build();

        final TextDeSerializer textDeSer = new TextDeSerializer();
        final XmlDeSerializer greetV1 = XmlDeSerializer.builder().add(GreetV1.class).version("1").build();
        final XmlDeSerializer greetV2 = XmlDeSerializer.builder().add(GreetV2.class).version("2").build();
        final XmlDeSerializer bookXml = XmlDeSerializer.builder().add(BookAddedEvent.class).build();

        final SerDeserializerRegistry registry = EscJacksonUtils
                .addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(EscJacksonUtils.MIME_TYPE), jacksonDeSer)
                .add(new SerializedDataType(BookAddedEvent.TYPE.asBaseType()), jacksonDeSer, jacksonDeSer.getMimeType())
                .add(new SerializedDataType(BookAddedEvent.TYPE.asBaseType()), (Deserializer) bookXml, bookXml.getMimeType())
                .add(new SerializedDataType(MyMeta.TYPE.asBaseType()), jacksonDeSer, jacksonDeSer.getMimeType())
                .add(new SerializedDataType("TextEvent"), textDeSer, textDeSer.getMimeType())
                .add(GreetV1.SER_TYPE, greetV1, greetV1.getMimeType())
                .add(GreetV1.SER_TYPE, (Deserializer) greetV2, greetV2.getMimeType())
                .build();

        // Finalize the mapper now that the registry (referenced by the ESC module) exists.
        mapperBuilder.registerModule(new EscJacksonModule(registry, registry));
        return registry;
    }

    /**
     * Creates a converter registry that up-casts a stored version 1 "GreetEvent" ({@link GreetV1}) to the
     * latest version 2 ({@link GreetV2}) on read.
     *
     * @return New instance.
     */
    public static ConverterRegistry converterRegistry() {
        return new SimpleConverterRegistry.Builder()
                .add(GreetV1.SER_TYPE, "1", "2", new GreetV1ToV2Converter())
                .build();
    }

    /**
     * Adds test types to the JSON-B config.
     *
     * @param jsonbConfig Config to register the types.
     * @param serializerRegistry Registry with serializers.
     * @param deserializerRegistry Registry with deserializers.
     */
    public static void register(JsonbConfig jsonbConfig, SerializerRegistry serializerRegistry, DeserializerRegistry deserializerRegistry) {
        jsonbConfig.withAdapters(EscJsonbUtils.createEscJsonbAdapters());
        jsonbConfig.withDeserializers(EscJsonbUtils.createEscJsonbDeserializers(serializerRegistry, deserializerRegistry));
        jsonbConfig.withSerializers(EscJsonbUtils.createEscJsonbSerializers(serializerRegistry, deserializerRegistry));
    }


}
