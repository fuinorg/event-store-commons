package org.fuin.esc.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.fuin.esc.api.IBase64Data;
import org.fuin.utils4j.TestOmitted;

import java.io.IOException;

/**
 * Serializes an {@link Base64Data} instance to JSON with Jackson.
 */
@TestOmitted("Already tested along with the other tests in this package")
public class Base64DataJacksonSerializer extends StdSerializer<Base64Data> {

    public Base64DataJacksonSerializer() {
        super(Base64Data.class);
    }

    @Override
    public void serialize(Base64Data base64Data, JsonGenerator generator,
                          SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeStringField(IBase64Data.EL_ROOT_NAME, base64Data.getEncoded());
        generator.writeEndObject();
    }

}
