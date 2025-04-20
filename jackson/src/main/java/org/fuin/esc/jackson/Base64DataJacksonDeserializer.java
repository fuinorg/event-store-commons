package org.fuin.esc.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.fuin.esc.api.IBase64Data;
import org.fuin.utils4j.TestOmitted;

import java.io.IOException;

/**
 * Deserializes JSON to an {@link Base64Data} instance with Jackson.
 */
@TestOmitted("Already tested along with the other tests in this package")
public class Base64DataJacksonDeserializer extends StdDeserializer<Base64Data> {

    public Base64DataJacksonDeserializer() {
        super(Base64Data.class);
    }

    @Override
    public Base64Data deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final JsonNode node = jp.getCodec().readTree(jp);
        final String base64Str = node.get(IBase64Data.EL_ROOT_NAME).asText();
        return new Base64Data(base64Str);
    }

}
