package org.fuin.esc.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.fuin.esc.api.IEscEvent;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.utils4j.TestOmitted;

import java.io.IOException;
import java.util.Objects;

/**
 * Serializes an {@link EscEvent} instance to JSON with Jackson.
 */
@TestOmitted("Already tested along with the other tests in this package")
public final class EscEventJacksonSerializer extends StdSerializer<EscEvent> {

    private final SerializerRegistry serializerRegistry;

    public EscEventJacksonSerializer(final SerializerRegistry serializerRegistry) {
        super(EscEvent.class);
        this.serializerRegistry = Objects.requireNonNull(serializerRegistry, "serializerRegistry==null");
    }

    @Override
    public void serialize(EscEvent escEvent,
                          JsonGenerator generator,
                          SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        if (escEvent != null) {
            generator.writeStringField(IEscEvent.EL_EVENT_ID, escEvent.getEventId());
            generator.writeStringField(IEscEvent.EL_EVENT_TYPE, escEvent.getEventType());
            if (escEvent.getData().getObj() instanceof Base64Data base64Data) {
                generator.writeObjectField(IEscEvent.EL_DATA, base64Data);
            } else {
                final SerializedDataType serDataType = new SerializedDataType(escEvent.getEventType());
                EscJacksonUtils.serialize(generator, serializerRegistry, serDataType,
                        IEscEvent.EL_DATA, escEvent.getData().getObj());
            }
            provider.defaultSerializeField(IEscEvent.EL_META_DATA, escEvent.getMeta().getObj(), generator);
        }
        generator.writeEndObject();
    }

}
