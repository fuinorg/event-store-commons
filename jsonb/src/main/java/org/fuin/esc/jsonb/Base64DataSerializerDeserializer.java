package org.fuin.esc.jsonb;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

import java.lang.reflect.Type;

/**
 * Adapter to use for JSON-B.
 */
public final class Base64DataSerializerDeserializer implements JsonbSerializer<Base64Data>, JsonbDeserializer<Base64Data> {

    @Override
    public Base64Data deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        parser.next();
        final String str = ctx.deserialize(String.class, parser);
        return new Base64Data(str);
    }

    @Override
    public void serialize(Base64Data obj, JsonGenerator generator, SerializationContext ctx) {
        generator.writeStartObject();
        generator.write(Base64Data.EL_ROOT_NAME, obj.getEncoded());
        generator.writeEnd();
    }

}
