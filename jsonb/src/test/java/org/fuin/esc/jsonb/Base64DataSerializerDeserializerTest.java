package org.fuin.esc.jsonb;

import org.fuin.objects4j.jsonb.JsonbProvider;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

/**
 * Test for the {@link Base64DataSerializerDeserializer} class.
 */
class Base64DataSerializerDeserializerTest extends AbstractTest {

    @Test
    public void testMarshalJsonBBase64() throws Exception {

        // PREPARE
        final String expectedJson = """
                {
                    "Base64": "eyAibXktZXZlbnQiOiB7ICJpZCI6ICAiNjg2MTZkOTAtY2Y3Mi00YzJhLWI5MTMtMzJiZjZlNjUwNmVkIiwgImRlc2NyaXB0aW9uIjogIkhlbGxvLCBKU09OISIgfSB9"
                }
                """;

        final Base64Data base64Data = new Base64Data("eyAibXktZXZlbnQiOiB7ICJpZCI6ICAiNjg2MTZkOTAtY2Y3Mi00YzJhLWI5MTMtMzJiZjZlNjUwNmVkIiwgImRlc2NyaXB0aW9uIjogIkhlbGxvLCBKU09OISIgfSB9");

        try (final JsonbProvider provider = getJsonbProvider()) {

            // TEST
            final String currentJson = provider.jsonb().toJson(base64Data);

            // VERIFY
            assertThatJson(currentJson).isEqualTo(expectedJson);

        }

    }

}