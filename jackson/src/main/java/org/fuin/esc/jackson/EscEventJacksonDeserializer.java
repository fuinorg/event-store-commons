package org.fuin.esc.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IEscEvent;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.objects4j.jackson.Objects4JacksonUtils;
import org.fuin.utils4j.TestOmitted;

import java.io.IOException;
import java.util.Objects;

/**
 * Deserializes JSON to an {@link EscEvent} instance with Jackson.
 */
@TestOmitted("Already tested along with the other tests in this package")
public final class EscEventJacksonDeserializer extends StdDeserializer<EscEvent> {

    private final DeserializerRegistry deserializerRegistry;

    public EscEventJacksonDeserializer(final DeserializerRegistry deserializerRegistry) {
        super(EscEvent.class);
        this.deserializerRegistry = Objects.requireNonNull(deserializerRegistry, "deserializerRegistry==null");
    }

    @Override
    public EscEvent deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final EscEvent escEvent = new EscEvent();

        final JsonNode node = jp.getCodec().readTree(jp);
        escEvent.setEventId(node.get(IEscEvent.EL_EVENT_ID).asText());
        escEvent.setEventType(node.get(IEscEvent.EL_EVENT_TYPE).asText());

        final JsonNode metaNode = node.get(IEscEvent.EL_META_DATA);

        final EscMeta escMeta = Objects4JacksonUtils.deserialize(jp, ctxt, EscMeta.class, metaNode);
        escEvent.setMeta(new DataWrapper(escMeta));

        final JsonNode dataNode = node.get(IEscEvent.EL_DATA);
        final JsonNode base64Node = dataNode.get(IBase64Data.EL_ROOT_NAME);
        if (base64Node == null) {
            final SerializedDataType dataType = new SerializedDataType(escEvent.getEventType());
            final EnhancedMimeType dataContentType = escMeta.getDataContentType();
            final Object data = EscJacksonUtils.deserialize(dataNode, dataType, dataContentType, deserializerRegistry);
            escEvent.setData(new DataWrapper(data));
        } else {
            escEvent.setData(new DataWrapper(new Base64Data(base64Node.asText())));
        }
        return escEvent;
    }

}
