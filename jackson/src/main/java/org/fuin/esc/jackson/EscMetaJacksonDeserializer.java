package org.fuin.esc.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SimpleTenantId;
import org.fuin.utils4j.TestOmitted;

import java.io.IOException;
import java.util.Objects;

/**
 * Adapter to use for JSON-B.
 */
@TestOmitted("Already tested along with the other tests in this package")
public final class EscMetaJacksonDeserializer extends StdDeserializer<EscMeta> {

    private final DeserializerRegistry deserializerRegistry;

    public EscMetaJacksonDeserializer(final DeserializerRegistry deserializerRegistry) {
        super(EscMeta.class);
        this.deserializerRegistry = Objects.requireNonNull(deserializerRegistry, "deserializerRegistry==null");
    }

    @Override
    public EscMeta deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final EscMeta escMeta = new EscMeta();

        final JsonNode node = jp.getCodec().readTree(jp);
        escMeta.setDataType(node.get(IEscMeta.EL_DATA_TYPE).asText());
        escMeta.setDataContentType(EnhancedMimeType.create(node.get(IEscMeta.EL_DATA_CONTENT_TYPE).asText()));
        if (node.has(IEscMeta.EL_TENANT)) {
            escMeta.setTenantId(new SimpleTenantId(node.get(IEscMeta.EL_TENANT).asText()));
        }
        if (node.has(IEscMeta.EL_META_TYPE)) {
            escMeta.setMetaType(node.get(IEscMeta.EL_META_TYPE).asText());
            escMeta.setMetaContentType(EnhancedMimeType.create(node.get(IEscMeta.EL_META_CONTENT_TYPE).asText()));
            final JsonNode base64Node = node.get(IBase64Data.EL_ROOT_NAME);
            if (base64Node == null) {
                final JsonNode metaNode = node.get(escMeta.getMetaType());
                final SerializedDataType metaType = new SerializedDataType(escMeta.getMetaType());
                final EnhancedMimeType metaContentType = escMeta.getMetaContentType();
                if (metaContentType == null) {
                    throw new IllegalStateException("Content type for meta is not defined");
                }
                final Object meta = EscJacksonUtils.deserialize(metaNode, metaType, metaContentType, deserializerRegistry);
                escMeta.setMeta(meta);
            } else {
                escMeta.setMeta(new Base64Data(base64Node.asText()));
            }
        }
        return escMeta;
    }

}
