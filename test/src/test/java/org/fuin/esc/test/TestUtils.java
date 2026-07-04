package org.fuin.esc.test;

import jakarta.json.bind.JsonbConfig;
import org.eclipse.yasson.FieldAccessStrategy;
import org.fuin.esc.api.*;
import org.fuin.esc.jaxb.EscJaxbUtils;
import org.fuin.esc.jaxb.XmlDeSerializer;
import org.fuin.esc.jsonb.EscJsonbUtils;
import org.fuin.esc.jsonb.JsonbSerDeserializer;
import org.fuin.esc.spi.TextDeSerializer;
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
