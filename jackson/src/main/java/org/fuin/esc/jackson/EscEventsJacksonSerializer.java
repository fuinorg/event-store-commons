package org.fuin.esc.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.fuin.esc.api.IEscEvent;
import org.fuin.utils4j.TestOmitted;

import java.io.IOException;

/**
 * Serializes an {@link EscEvents} instance to JSON with Jackson.
 */
@TestOmitted("Already tested along with the other tests in this package")
public final class EscEventsJacksonSerializer extends StdSerializer<EscEvents> {

    public EscEventsJacksonSerializer() {
        super(EscEvents.class);
    }

    @Override
    public void serialize(EscEvents escEvents, JsonGenerator generator,
                          SerializerProvider provider) throws IOException {
        if (escEvents == null) {
            return;
        }
        generator.writeStartArray();
        for (final IEscEvent event : escEvents.getList()) {
            provider.defaultSerializeValue(event, generator);
        }
        generator.writeEndArray();
    }

}
