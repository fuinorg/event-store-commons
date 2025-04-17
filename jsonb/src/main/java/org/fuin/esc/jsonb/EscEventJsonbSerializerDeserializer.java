package org.fuin.esc.jsonb;

import jakarta.json.JsonObject;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import org.fuin.esc.api.Deserializer;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.DeserializerRegistryRequired;
import org.fuin.esc.api.SerializedDataType;

import java.lang.reflect.Type;

/**
 * Adapter to use for JSON-B.
 */
public final class EscEventJsonbSerializerDeserializer implements JsonbSerializer<EscEvent>,
        JsonbDeserializer<EscEvent>, DeserializerRegistryRequired {

    private DeserializerRegistry registry;

    @Override
    public EscEvent deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        final EscEvent escEvent = new EscEvent();
        JsonObject content = null;
        while (parser.hasNext()) {
            final JsonParser.Event event = parser.next();
            if (event == JsonParser.Event.KEY_NAME) {
                final String field = parser.getString();
                switch (field) {
                    case EscEvent.EL_EVENT_ID:
                        escEvent.setEventId(ctx.deserialize(String.class, parser));
                        break;
                    case EscEvent.EL_EVENT_TYPE:
                        escEvent.setEventType(ctx.deserialize(String.class, parser));
                        break;
                    case EscEvent.EL_META_DATA:
                        parser.next(); // Skip key and deserialize object
                        escEvent.setMeta(new DataWrapper(ctx.deserialize(EscMeta.class, parser)));
                        break;
                    case EscEvent.EL_DATA:
                        parser.next(); // Skip key and deserialize object
                        content = ctx.deserialize(JsonObject.class, parser);
                        break;
                    default:
                        // ignore
                        break;
                }
            }
        }

        // Handle data at the end, because metadata is only safely available at the end of the process
        if (content == null) {
            throw new IllegalStateException("Expected content to be set, but was never processed during parse process");
        }
        if (content.containsKey(Base64Data.EL_ROOT_NAME)) {
            escEvent.setData(new DataWrapper(new Base64Data(content.getString(Base64Data.EL_ROOT_NAME))));
        } else {
            if (escEvent.getMeta() == null) {
                throw new IllegalStateException("Expected 'meta' to be set, but was never processed during parse process");
            }
            if (!(escEvent.getMeta().getObj() instanceof EscMeta escMeta)) {
                throw new IllegalStateException("Expected 'meta.object' to be of type 'EscMeta', but was: " + escEvent.getMeta().getObj());
            }
            final Deserializer deserializer = registry.getDeserializer(new SerializedDataType(escEvent.getEventType()),
                    escMeta.getDataContentType());
            final Object obj = deserializer.unmarshal(content, new SerializedDataType(escEvent.getEventType()),
                    escMeta.getDataContentType());
            escEvent.setData(new DataWrapper(obj));
        }

        return escEvent;
    }

    @Override
    public void serialize(EscEvent escEvent, JsonGenerator generator, SerializationContext ctx) {
        generator.writeStartObject();
        if (escEvent != null) {
            generator.write(EscEvent.EL_EVENT_ID, escEvent.getEventId());
            generator.write(EscEvent.EL_EVENT_TYPE, escEvent.getEventType());
            if (escEvent.getData().getObj() instanceof Base64Data base64Data) {
                generator.writeStartObject(EscEvent.EL_DATA);
                generator.write(Base64Data.EL_ROOT_NAME, base64Data.getEncoded());
                generator.writeEnd();
            } else {
                ctx.serialize(EscEvent.EL_DATA, escEvent.getData().getObj(), generator);
            }
            ctx.serialize(EscEvent.EL_META_DATA, escEvent.getMeta().getObj(), generator);
        }
        generator.writeEnd();
    }

    @Override
    public void setRegistry(final DeserializerRegistry registry) {
        this.registry = registry;

    }

}
