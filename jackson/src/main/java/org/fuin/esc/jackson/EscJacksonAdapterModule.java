package org.fuin.esc.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.jackson.ValueObjectStringJacksonDeserializer;
import org.fuin.objects4j.jackson.ValueObjectStringJacksonSerializer;
import org.fuin.utils4j.TestOmitted;

import java.util.Objects;

/**
 * Module that registers the adapters for the package.
 */
@TestOmitted("Trivial")
public class EscJacksonAdapterModule extends Module {

    private final DeserializerRegistry deserializerRegistry;

    private final SerializerRegistry serializerRegistry;

    /**
     * Constructor with mandatory data.
     *
     * @param deserializerRegistry Deserializer registry.
     * @param serializerRegistry   Serializer registry.
     */
    public EscJacksonAdapterModule(final DeserializerRegistry deserializerRegistry,
                                   final SerializerRegistry serializerRegistry) {
        this.deserializerRegistry = Objects.requireNonNull(deserializerRegistry, "deserializerRegistry==null");
        this.serializerRegistry = Objects.requireNonNull(serializerRegistry, "serializerRegistry==null");
    }


    @Override
    public String getModuleName() {
        return "Objects4JModule";
    }

    @Override
    public void setupModule(SetupContext context) {

        final SimpleSerializers serializers = new SimpleSerializers();
        serializers.addSerializer(Base64Data.class, new Base64DataJacksonSerializer());
        serializers.addSerializer(EscEvents.class, new EscEventsJacksonSerializer());
        serializers.addSerializer(EscEvent.class, new EscEventJacksonSerializer(serializerRegistry));
        serializers.addSerializer(EscMeta.class, new EscMetaJacksonSerializer(serializerRegistry));
        serializers.addSerializer(EventId.class, new ValueObjectStringJacksonSerializer<>(EventId.class));
        serializers.addSerializer(TypeName.class, new ValueObjectStringJacksonSerializer<>(TypeName.class));
        context.addSerializers(serializers);

        final SimpleDeserializers deserializers = new SimpleDeserializers();
        deserializers.addDeserializer(Base64Data.class, new Base64DataJacksonDeserializer());
        deserializers.addDeserializer(EscEvents.class, new EscEventsJacksonDeserializer());
        deserializers.addDeserializer(EscEvent.class, new EscEventJacksonDeserializer(deserializerRegistry));
        deserializers.addDeserializer(EscMeta.class, new EscMetaJacksonDeserializer(deserializerRegistry));
        deserializers.addDeserializer(EventId.class, new ValueObjectStringJacksonDeserializer<>(EventId.class, EventId::valueOf));
        deserializers.addDeserializer(TypeName.class, new ValueObjectStringJacksonDeserializer<>(TypeName.class, TypeName::valueOf));
        context.addDeserializers(deserializers);
    }

    @Override
    public Version version() {
        // Don't forget to change from release to SNAPSHOT and back!
        return new Version(0, 9, 0, "SNAPSHOT",
        "org.fuin.esc", "esc-jackson");
    }

}