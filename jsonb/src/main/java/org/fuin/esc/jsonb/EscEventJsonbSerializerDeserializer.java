package org.fuin.esc.jsonb;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.DeserializerRegistryRequired;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IEscEvent;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistryRequired;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.api.SerializerRegistryRequired;
import org.fuin.utils4j.TestOmitted;

import java.lang.reflect.Type;

/**
 * Adapter to use for JSON-B.
 */
@TestOmitted("Already tested along with the other tests in this package")
public final class EscEventJsonbSerializerDeserializer implements JsonbSerializer<EscEvent>,
        JsonbDeserializer<EscEvent>, SerializerRegistryRequired, DeserializerRegistryRequired {

    private SerializerRegistry serializerRegistry;

    private DeserializerRegistry deserializerRegistry;

    @Override
    public EscEvent deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        final EscEvent escEvent = new EscEvent();
        JsonValue content = null;
        while (parser.hasNext()) {
            final JsonParser.Event event = parser.next();
            if (event == JsonParser.Event.KEY_NAME) {
                final String field = parser.getString();
                switch (field) {
                    case IEscEvent.EL_EVENT_ID:
                        escEvent.setEventId(ctx.deserialize(String.class, parser));
                        break;
                    case IEscEvent.EL_EVENT_TYPE:
                        escEvent.setEventType(ctx.deserialize(String.class, parser));
                        break;
                    case IEscEvent.EL_META_DATA:
                        parser.next(); // Skip key and deserialize object
                        escEvent.setMeta(new DataWrapper(ctx.deserialize(EscMeta.class, parser)));
                        break;
                    case IEscEvent.EL_DATA:
                        parser.next(); // Skip key and deserialize object
                        // We cannot directly deserialize into target type as meta information might not be
                        // set at this point in time as the order of the tags is undefined.
                        content = ctx.deserialize(JsonValue.class, parser);
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
        if (content.getValueType() == JsonValue.ValueType.OBJECT
                && ((JsonObject) content).containsKey(IBase64Data.EL_ROOT_NAME)) {
            escEvent.setData(new DataWrapper(new Base64Data(((JsonObject) content).getString(IBase64Data.EL_ROOT_NAME))));
        } else {
            if (escEvent.getMeta() == null) { //NOSONAR Can unfortunately be null because it's not set above...
                throw new IllegalStateException("Expected 'meta' to be set, but was never processed during parse process");
            }
            if (!(escEvent.getMeta().getObj() instanceof EscMeta escMeta)) {
                throw new IllegalStateException("Expected 'meta.object' to be of type 'EscMeta', but was: " + escEvent.getMeta().getObj());
            }

            final SerializedDataType dataType = new SerializedDataType(escEvent.getEventType());
            final EnhancedMimeType dataContentType = escMeta.getDataContentType();
            final Object data = EscJsonbUtils.deserialize(content, dataType, dataContentType, deserializerRegistry);
            escEvent.setData(new DataWrapper(data));
        }

        return escEvent;
    }

    @Override
    public void serialize(EscEvent escEvent, JsonGenerator generator, SerializationContext ctx) {
        generator.writeStartObject();
        if (escEvent != null) {
            generator.write(IEscEvent.EL_EVENT_ID, escEvent.getEventId());
            generator.write(IEscEvent.EL_EVENT_TYPE, escEvent.getEventType());
            if (escEvent.getData().getObj() instanceof Base64Data base64Data) {
                generator.writeStartObject(IEscEvent.EL_DATA);
                generator.write(IBase64Data.EL_ROOT_NAME, base64Data.getEncoded());
                generator.writeEnd();
            } else {
                final SerializedDataType serDataType = new SerializedDataType(escEvent.getEventType());
                EscJsonbUtils.serialize(generator, ctx, serializerRegistry,
                        serDataType, IEscEvent.EL_DATA, escEvent.getData().getObj());
            }
            ctx.serialize(IEscEvent.EL_META_DATA, escEvent.getMeta().getObj(), generator);
        }
        generator.writeEnd();
    }

    @Override
    public void setRegistry(final DeserializerRegistry registry) {
        this.deserializerRegistry = registry;
    }

    @Override
    public void setRegistry(final SerializerRegistry registry) {
        this.serializerRegistry = registry;
    }

}
