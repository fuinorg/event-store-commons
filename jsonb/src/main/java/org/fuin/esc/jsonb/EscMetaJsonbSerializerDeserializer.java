package org.fuin.esc.jsonb;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistryRequired;
import org.fuin.utils4j.TestOmitted;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Adapter to use for JSON-B.
 */
@TestOmitted("Already tested along with the other tests in this package")
public final class EscMetaJsonbSerializerDeserializer
        implements JsonbSerializer<EscMeta>, JsonbDeserializer<EscMeta>, SerializedDataTypeRegistryRequired {

    private SerializedDataTypeRegistry registry;

    @Override
    public EscMeta deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
        final EscMeta escMeta = new EscMeta();
        while (parser.hasNext()) {
            final JsonParser.Event event = parser.next();
            if (event == JsonParser.Event.KEY_NAME) {
                final String field = parser.getString();
                switch (field) {
                    case IEscMeta.EL_DATA_TYPE:
                        escMeta.setDataType(ctx.deserialize(String.class, parser));
                        break;
                    case IEscMeta.EL_DATA_CONTENT_TYPE:
                        escMeta.setDataContentType(EnhancedMimeType.create(ctx.deserialize(String.class, parser)));
                        break;
                    case IEscMeta.EL_META_TYPE:
                        escMeta.setMetaType(ctx.deserialize(String.class, parser));
                        break;
                    case IEscMeta.EL_META_CONTENT_TYPE:
                        escMeta.setMetaContentType(EnhancedMimeType.create(ctx.deserialize(String.class, parser)));
                        break;
                    default:
                        // meta
                        if (field.equals(IBase64Data.EL_ROOT_NAME)) {
                            escMeta.setMeta(new Base64Data(ctx.deserialize(String.class, parser)));
                        } else {
                            final Class<?> clasz = registry.findClass(new SerializedDataType(escMeta.getMetaType()));
                            escMeta.setMeta(ctx.deserialize(clasz, parser));
                        }
                        break;
                }
            }
        }
        return escMeta;
    }

    @Override
    public void serialize(EscMeta escMeta, JsonGenerator generator, SerializationContext ctx) {

        generator.writeStartObject();
        generator.write(IEscMeta.EL_DATA_TYPE, escMeta.getDataType());
        generator.write(IEscMeta.EL_DATA_CONTENT_TYPE, escMeta.getDataContentType().toString());
        if (escMeta.getMeta() != null) { //NOSONAR Can unfortunately be null because it's not set above...
            generator.write(IEscMeta.EL_META_TYPE, escMeta.getMetaType());
            generator.write(IEscMeta.EL_META_CONTENT_TYPE, Objects.requireNonNull(escMeta.getMetaContentType()).toString());
            if (escMeta.getMeta() instanceof Base64Data base64data) {
                generator.write(IBase64Data.EL_ROOT_NAME, base64data.getEncoded());
            } else {
                ctx.serialize(escMeta.getMetaType(), escMeta.getMeta(), generator);
            }
        }
        generator.writeEnd();

    }

    @Override
    public void setRegistry(final SerializedDataTypeRegistry registry) {
        this.registry = registry;
    }

}
