package org.fuin.esc.jackson;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.objects4j.common.Contract;
import org.fuin.utils4j.TestOmitted;

import java.nio.charset.StandardCharsets;

/**
 * Helper methods for the test package.
 */
@TestOmitted("This is only a test class")
final class TestUtils {

    private TestUtils() {
    }

    /**
     * Creates a {@link JacksonDeSerializer} with standard settings.
     *
     * @return New instance.
     */
    public static JacksonDeSerializer createJacksonDeSerializer() {
        return JacksonDeSerializer.builder()
                .withSerializers(EscJacksonUtils.createEscJacksonSerializers())
                .withDeserializers(EscJacksonUtils.createEscJacksonDeserializers())
                .withEncoding(StandardCharsets.UTF_8)
                .build();
    }

    /**
     * Creates a registry that connects the type with the appropriate serializer and de-serializer.
     *
     * @param typeRegistry Type registry (Mapping from type name to class).
     * @param jacksonDeSer   JSON-B serializer/deserializer to use.
     */
    public static void initSerDeserializerRegistry(@NotNull SerializedDataTypeRegistry typeRegistry,
                                                   @NotNull JacksonDeSerializer jacksonDeSer) {

        Contract.requireArgNotNull("typeRegistry", typeRegistry);
        Contract.requireArgNotNull("jacksonDeSer", jacksonDeSer);

        SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

        // Base types always needed
        registry.add(EscEvents.SER_TYPE, jacksonDeSer.getMimeType().getBaseType(), jacksonDeSer);
        registry.add(EscEvent.SER_TYPE, jacksonDeSer.getMimeType().getBaseType(), jacksonDeSer);
        registry.add(EscMeta.SER_TYPE, jacksonDeSer.getMimeType().getBaseType(), jacksonDeSer);

        // User defined types
        registry.add(MyMeta.SER_TYPE, jacksonDeSer.getMimeType().getBaseType(), jacksonDeSer);
        registry.add(MyEvent.SER_TYPE, jacksonDeSer.getMimeType().getBaseType(), jacksonDeSer);

        jacksonDeSer.init(typeRegistry, registry, registry);

    }

}
