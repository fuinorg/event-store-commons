package org.fuin.esc.jaxb;

import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistry;

import java.nio.charset.StandardCharsets;

/**
 * Utilities for the JAX-B serialization module.
 */
public final class EscJaxbUtils {

    /**
     * Default mime type for the module.
     */
    public static final EnhancedMimeType MIME_TYPE = EnhancedMimeType.create("application", "xml", StandardCharsets.UTF_8);

    /**
     * Private utility constructor.
     */
    private EscJaxbUtils() {
        throw new UnsupportedOperationException("Creating instances of a utility class is not allowed.");
    }

    /**
     * Creates a builder that is initialized with default settings.
     *
     * @return Builder.
     */
    public static XmlDeSerializer.Builder xmlDeSerializerBuilder() {
        return XmlDeSerializer.builder()
                .fragment(true)
                .add(EscEvents.class)
                .add(EscEvent.class)
                .add(EscMeta.class)
                .add(Base64Data.class);
    }

    /**
     * Adds the standard ESC types to the registry.
     *
     * @param builder Builder to add the standard types to.
     * @param <T>     Type of the registry.
     * @param <B>     Type of the registry builder.
     * @return The builder.
     */
    public static <T extends SerializedDataTypeRegistry, B extends SerializedDataTypeRegistry.Builder<T, B>> B addEscTypes(B builder) {
        builder.add(EscEvents.SER_TYPE, EscEvents.class);
        builder.add(EscEvent.SER_TYPE, EscEvent.class);
        builder.add(EscMeta.SER_TYPE, EscMeta.class);
        builder.add(Base64Data.SER_TYPE, Base64Data.class);
        return builder;
    }

    /**
     * Adds the standard ESC types to the registry.
     *
     * @param builder         Builder to add the standard types to.
     * @param serDeserializer Serializer/Deserializer to use.
     * @param <T>             Type of the registry.
     * @param <B>             Type of the registry builder.
     * @return The builder.
     */
    public static <T extends SerDeserializerRegistry, B extends SerDeserializerRegistry.Builder<T, B>> B addEscSerDeserializer(B builder, XmlDeSerializer serDeserializer) {
        builder.add(EscEvents.SER_TYPE, serDeserializer, serDeserializer.getMimeType());
        builder.add(EscEvent.SER_TYPE, serDeserializer, serDeserializer.getMimeType());
        builder.add(EscMeta.SER_TYPE, serDeserializer, serDeserializer.getMimeType());
        builder.add(Base64Data.SER_TYPE, serDeserializer, serDeserializer.getMimeType());
        return builder;
    }

}
