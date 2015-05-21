package org.fuin.esc.spi;

import static org.fest.assertions.Assertions.assertThat;

import javax.activation.MimeTypeParseException;
import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JsonDeSerializer} class.
 */
// CHECKSTYLE:OFF Test
public class JsonDeSerializerTest {

    private JsonDeSerializer testee;

    @Before
    public void setup() throws MimeTypeParseException {
        testee = new JsonDeSerializer(
                "application/xml;version=1.0.2;encoding=utf-8");
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testMarshalUnmarshal() {

        // PREPARE
        final JsonObject original = Json.createObjectBuilder()
                .add("name", "Peter").add("age", 21).build();

        // TEST
        final byte[] data = testee.marshal(original);
        final JsonObject copy = testee.unmarshal(data);

        // VERIFY
        assertThat(copy.keySet()).contains("name", "age");
        assertThat(copy.getString("name")).isEqualTo("Peter");
        assertThat(copy.getInt("age")).isEqualTo(21);

    }

}
