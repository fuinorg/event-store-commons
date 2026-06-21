package org.fuin.esc.jsonb;

import jakarta.json.bind.JsonbConfig;
import org.eclipse.yasson.FieldAccessStrategy;
import org.fuin.esc.api.*;
import org.fuin.objects4j.jsonb.JsonbProvider;
import org.fuin.utils4j.TestOmitted;

import java.nio.charset.StandardCharsets;

import static org.fuin.esc.jsonb.EscJsonbUtils.*;

/**
 * Helper methods for the test package.
 */
@TestOmitted("This is only a test class")
final class TestUtils {

    private TestUtils() {
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
     * Creates a pre-configured serializer/deserializer.
     *
     * @param jsonbProvider JSON-B provider.
     * @param typeRegistry  Type registry.
     * @return New instance.
     */
    public static JsonbSerDeserializer createSerDeserializer(
            final JsonbProvider jsonbProvider,
            final SerializedDataTypeRegistry typeRegistry) {

        return new JsonbSerDeserializer(jsonbProvider, typeRegistry, StandardCharsets.UTF_8);
    }

    public static SerializedDataTypeRegistry createSerializedDataTypeRegistry() {
        return addEscTypes(new SimpleSerializedDataTypeRegistry.Builder())
                .add(MyEvent.SER_TYPE, MyEvent.class)
                .add(MyMeta.SER_TYPE, MyMeta.class)
                .build();
    }

    public static SerDeserializerRegistry createSerDeserializerRegistry(JsonbSerDeserializer serDeserializer) {
        return addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(MIME_TYPE), serDeserializer)
                .add(MyMeta.SER_TYPE, serDeserializer, serDeserializer.getMimeType())
                .add(MyEvent.SER_TYPE, serDeserializer, serDeserializer.getMimeType())
                .build();
    }

    public static void register(JsonbConfig jsonbConfig, SerializerRegistry serializerRegistry, DeserializerRegistry deserializerRegistry) {
        jsonbConfig.withAdapters(EscJsonbUtils.createEscJsonbAdapters());
        jsonbConfig.withDeserializers(EscJsonbUtils.createEscJsonbDeserializers(serializerRegistry, deserializerRegistry));
        jsonbConfig.withSerializers(EscJsonbUtils.createEscJsonbSerializers(serializerRegistry, deserializerRegistry));
    }
}
