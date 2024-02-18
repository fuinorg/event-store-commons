package org.fuin.esc.jsonb;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import org.fuin.esc.api.IEscEvent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to use for JSON-B.
 */
public final class EscEventsJsonbSerializerDeserializer implements JsonbSerializer<EscEvents>, JsonbDeserializer<EscEvents> {

    @Override
    public EscEvents deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        final List<EscEvent> events = new ArrayList<>();
        while (parser.hasNext()) {
            final JsonParser.Event event = parser.next();
            if (event == JsonParser.Event.START_OBJECT) {
                events.add(ctx.deserialize(EscEvent.class, parser));
            }
        }
        return new EscEvents(events);
    }

    @Override
    public void serialize(EscEvents escEvents, JsonGenerator generator, SerializationContext ctx) {
        if (escEvents == null) {
            return;
        }
        generator.writeStartArray();
        for (final IEscEvent event : escEvents.getList()) {
            ctx.serialize(event, generator);
        }
        generator.writeEnd();
    }

}
