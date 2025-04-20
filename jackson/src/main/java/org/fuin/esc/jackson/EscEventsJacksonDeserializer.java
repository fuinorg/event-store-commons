package org.fuin.esc.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.fuin.utils4j.TestOmitted;

import java.io.IOException;

/**
 * Deserializes JSON to an {@link EscEvents} instance with Jackson.
 */
@TestOmitted("Already tested along with the other tests in this package")
public final class EscEventsJacksonDeserializer extends StdDeserializer<EscEvents> {

    public EscEventsJacksonDeserializer() {
        super(EscEvents.class);
    }

    @Override
    public EscEvents deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final EscEvent[] events = jp.readValueAs(EscEvent[].class);
        return new EscEvents(events);
    }

}
