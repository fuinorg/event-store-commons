package org.fuin.esc.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.objects4j.jackson.ImmutableObjectMapper;
import org.fuin.utils4j.TestOmitted;

import java.nio.charset.StandardCharsets;

import static org.fuin.esc.jackson.EscJacksonUtils.MIME_TYPE;
import static org.fuin.esc.jackson.EscJacksonUtils.addEscSerDeserializer;
import static org.fuin.esc.jackson.EscJacksonUtils.addEscTypes;

/**
 * Helper methods for the test package.
 */
@TestOmitted("This is only a test class")
final class TestUtils {

    private TestUtils() {
    }

    /**
     * Creates a builder.
     *
     * @return Builder.
     */
    public static ImmutableObjectMapper.Builder createMapperBuilder() {

        return new ImmutableObjectMapper.Builder(new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT));

    }

    /**
     * Creates a pre-configured serializer/deserializer.
     *
     * @param mapperProvider Mapper provider.
     * @param typeRegistry   Type registry.
     * @return New instance.
     */
    public static JacksonSerDeserializer createSerDeserializer(
            final ImmutableObjectMapper.Provider mapperProvider,
            final SerializedDataTypeRegistry typeRegistry) {

        return new JacksonSerDeserializer.Builder()
                .withObjectMapper(mapperProvider)
                .withTypeRegistry(typeRegistry)
                .withEncoding(StandardCharsets.UTF_8)
                .build();

    }

    public static SerializedDataTypeRegistry createSerializedDataTypeRegistry() {
        return addEscTypes(new SimpleSerializedDataTypeRegistry.Builder())
                .add(MyEvent.SER_TYPE, MyEvent.class)
                .add(MyMeta.SER_TYPE, MyMeta.class)
                .build();
    }

    public static SerDeserializerRegistry createSerDeserializerRegistry(JacksonSerDeserializer serDeserializer) {
        return addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(MIME_TYPE), serDeserializer)
                .add(MyMeta.SER_TYPE, serDeserializer, serDeserializer.getMimeType())
                .add(MyEvent.SER_TYPE, serDeserializer, serDeserializer.getMimeType())
                .build();
    }


}
