package org.fuin.esc.esgrpc;

import jakarta.json.bind.JsonbConfig;
import org.eclipse.yasson.FieldAccessStrategy;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.jsonb.EscJsonbUtils;
import org.fuin.esc.jsonb.JsonbSerDeserializer;
import org.fuin.objects4j.jsonb.JsonbProvider;
import org.fuin.utils4j.TestOmitted;

import java.nio.charset.StandardCharsets;


/**
 * Helper methods for the test package.
 */
@TestOmitted("This is only a test class")
final class TestUtils {

    private static final EnhancedMimeType XML_MIME_TYPE = EnhancedMimeType.create("application", "xml", StandardCharsets.UTF_8);

    private TestUtils() {
    }

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

    public static void register(JsonbConfig jsonbConfig, SerializerRegistry serializerRegistry, DeserializerRegistry deserializerRegistry) {
        jsonbConfig.withAdapters(EscJsonbUtils.createEscJsonbAdapters());
        jsonbConfig.withDeserializers(EscJsonbUtils.createEscJsonbDeserializers(serializerRegistry, deserializerRegistry));
        jsonbConfig.withSerializers(EscJsonbUtils.createEscJsonbSerializers(serializerRegistry, deserializerRegistry));
    }
}
