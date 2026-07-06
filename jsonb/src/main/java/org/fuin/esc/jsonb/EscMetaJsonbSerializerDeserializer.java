package org.fuin.esc.jsonb;

import jakarta.json.JsonArray;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import org.fuin.esc.api.*;
import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.utils4j.TestOmitted;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapter to use for JSON-B.
 */
@ThreadSafe
@TestOmitted("Already tested along with the other tests in this package")
public final class EscMetaJsonbSerializerDeserializer implements JsonbSerializer<EscMeta>, JsonbDeserializer<EscMeta> {

    private final SerializerRegistry serializerRegistry;

    private final DeserializerRegistry deserializerRegistry;

    public EscMetaJsonbSerializerDeserializer(SerializerRegistry serializerRegistry, DeserializerRegistry deserializerRegistry) {
        this.serializerRegistry = Objects.requireNonNull(serializerRegistry, "serializerRegistry==null");
        this.deserializerRegistry = Objects.requireNonNull(deserializerRegistry, "deserializerRegistry==null");
    }

    @Override
    public EscMeta deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
        final EscMeta escMeta = new EscMeta();
        while (parser.hasNext()) {
            final JsonParser.Event event = parser.next();
            if (event == JsonParser.Event.KEY_NAME) {
                final String field = parser.getString();
                // Ignore fields like "$traceId" / "$spanId"
                if (field != null && !field.startsWith("$")) {
                    switch (field) {
                        case IEscMeta.EL_DATA_TYPE:
                            escMeta.setDataType(ctx.deserialize(String.class, parser));
                            break;
                        case IEscMeta.EL_DATA_CONTENT_TYPE:
                            escMeta.setDataContentType(Objects.requireNonNull(EnhancedMimeType.create(ctx.deserialize(String.class, parser))));
                            break;
                        case IEscMeta.EL_TENANT:
                            escMeta.setTenantId(new SimpleTenantId(ctx.deserialize(String.class, parser)));
                            break;
                        case IEscMeta.EL_META_TYPE:
                            escMeta.setMetaType(ctx.deserialize(String.class, parser));
                            break;
                        case IEscMeta.EL_META_CONTENT_TYPE:
                            escMeta.setMetaContentType(EnhancedMimeType.create(ctx.deserialize(String.class, parser)));
                            break;
                        default:
                            if (field.equals(IEscMeta.EL_CATEGORIES)) {
                                // Consume the array tokens directly (cursor is at the key): relying on
                                // ctx.deserialize(JsonValue) here overshoots and skips the following field.
                                final List<String> categories = new ArrayList<>();
                                if (parser.next() == JsonParser.Event.START_ARRAY) {
                                    while (parser.hasNext()) {
                                        final JsonParser.Event element = parser.next();
                                        if (element == JsonParser.Event.END_ARRAY) {
                                            break;
                                        }
                                        if (element == JsonParser.Event.VALUE_STRING) {
                                            categories.add(parser.getString());
                                        }
                                    }
                                }
                                escMeta.setCategories(categories);
                            } else if (field.equals(IBase64Data.EL_ROOT_NAME)) {
                                escMeta.setMeta(new Base64Data(ctx.deserialize(String.class, parser)));
                            } else {
                                if (escMeta.getMetaContentType() == null) {
                                    throw new IllegalStateException("Content type for meta is not defined");
                                }
                                parser.next();
                                final JsonValue content = ctx.deserialize(JsonValue.class, parser);
                                final SerializedDataType metaType = new SerializedDataType(Objects.requireNonNull(escMeta.getMetaType()));
                                final EnhancedMimeType metaContentType = escMeta.getMetaContentType();
                                final Object meta = EscJsonbUtils.deserialize(content, metaType, metaContentType, deserializerRegistry);
                                escMeta.setMeta(meta);
                            }
                            break;
                    }
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
        if (escMeta.getTenantId() != null) {
            generator.write(IEscMeta.EL_TENANT, escMeta.getTenantId().asString());
        }
        if (!escMeta.getCategories().isEmpty()) {
            generator.writeStartArray(IEscMeta.EL_CATEGORIES);
            for (final String category : escMeta.getCategories()) {
                generator.write(category);
            }
            generator.writeEnd();
        }
        if (escMeta.getMeta() != null) { //NOSONAR Can unfortunately be null because it's not set above...
            generator.write(IEscMeta.EL_META_TYPE, escMeta.getMetaType());
            generator.write(IEscMeta.EL_META_CONTENT_TYPE, Objects.requireNonNull(escMeta.getMetaContentType()).toString());
            if (escMeta.getMeta() instanceof Base64Data base64data) {
                generator.write(IBase64Data.EL_ROOT_NAME, base64data.getEncoded());
            } else {
                final SerializedDataType serDataType = new SerializedDataType(Objects.requireNonNull(escMeta.getMetaType()));
                EscJsonbUtils.serialize(generator, ctx, serializerRegistry,
                        serDataType, Objects.requireNonNull(escMeta.getMetaType()), escMeta.getMeta());
            }
        }
        generator.writeEnd();

    }

}
