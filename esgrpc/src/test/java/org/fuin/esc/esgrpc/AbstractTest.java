package org.fuin.esc.esgrpc;

import jakarta.json.bind.JsonbConfig;
import org.fuin.objects4j.jsonb.JsonbProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base test that inigtializes registry and mapper.
 */
public abstract class AbstractTest {

    private JsonbConfig jsonbConfig;

    private JsonbProvider jsonbProvider;

    @BeforeEach
    public void setup() {
        jsonbConfig = TestUtils.createJsonbConfig();
        jsonbProvider = new JsonbProvider(jsonbConfig);
    }

    @AfterEach
    public void teardown() {
        jsonbProvider = null;
        jsonbConfig = null;
    }

    public JsonbConfig getJsonbConfig() {
        return jsonbConfig;
    }

    public JsonbProvider getJsonbProvider() {
        return jsonbProvider;
    }


}
