package org.fuin.esc.test;

import jakarta.json.bind.JsonbConfig;
import org.eclipse.yasson.FieldAccessStrategy;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.jaxb.EscJaxbUtils;
import org.fuin.esc.jaxb.XmlDeSerializer;
import org.fuin.esc.jsonb.EscJsonbUtils;
import org.fuin.esc.jsonb.JsonbSerDeserializer;
import org.fuin.esc.spi.TextDeSerializer;
import org.fuin.esc.test.examples.BookAddedEvent;
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
        return EscJaxbUtils.addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(EscJaxbUtils.MIME_TYPE), xmlDeSer)
                .add(new SerializedDataType(BookAddedEvent.TYPE.asBaseType()), xmlDeSer, xmlDeSer.getMimeType())
                .add(new SerializedDataType(MyMeta.TYPE.asBaseType()), jsonbDeSer, jsonbDeSer.getMimeType())
                .add(new SerializedDataType("TextEvent"), textDeSer, textDeSer.getMimeType())
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
