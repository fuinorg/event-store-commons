package org.fuin.esc.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.utils4j.TestOmitted;

import java.io.IOException;
import java.util.Objects;

/**
 * Adapter to use for JSON-B.
 */
@TestOmitted("Already tested along with the other tests in this package")
public final class EscMetaJacksonSerializer extends StdSerializer<EscMeta> {

    private final SerializerRegistry serializerRegistry;

    public EscMetaJacksonSerializer(final SerializerRegistry serializerRegistry) {
        super(EscMeta.class);
        this.serializerRegistry = Objects.requireNonNull(serializerRegistry, "serializerRegistry==null");
    }

    @Override
    public void serialize(EscMeta escMeta, JsonGenerator generator,
                          SerializerProvider provider) throws IOException {

        generator.writeStartObject();
        generator.writeStringField(IEscMeta.EL_DATA_TYPE, escMeta.getDataType());
        generator.writeStringField(IEscMeta.EL_DATA_CONTENT_TYPE, escMeta.getDataContentType().toString());
        if (escMeta.getTenantId() != null) {
            generator.writeStringField(IEscMeta.EL_TENANT, escMeta.getTenantId().asString());
        }
        if (escMeta.getMeta() != null) {
            generator.writeStringField(IEscMeta.EL_META_TYPE, escMeta.getMetaType());
            generator.writeStringField(IEscMeta.EL_META_CONTENT_TYPE, Objects.requireNonNull(escMeta.getMetaContentType()).toString());
            if (escMeta.getMeta() instanceof Base64Data base64data) {
                generator.writeStringField(IBase64Data.EL_ROOT_NAME, base64data.getEncoded());
            } else {
                provider.defaultSerializeField(escMeta.getMetaType(), escMeta.getMeta(), generator);
                final SerializedDataType serDataType = new SerializedDataType(escMeta.getMetaType());
                EscJacksonUtils.serialize(generator, serializerRegistry,
                        serDataType, escMeta.getMetaType(), escMeta.getMeta());
            }
        }
        generator.writeEndObject();

    }

}
