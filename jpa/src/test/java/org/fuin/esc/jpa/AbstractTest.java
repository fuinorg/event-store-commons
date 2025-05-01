package org.fuin.esc.jpa;

import jakarta.json.bind.JsonbConfig;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.jsonb.JsonbSerDeserializer;
import org.fuin.objects4j.jsonb.JsonbProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base test that inigtializes registry and mapper.
 */
public abstract class AbstractTest {

    private SerializedDataTypeRegistry typeRegistry;

    private JsonbConfig jsonbConfig;

    private JsonbProvider jsonbProvider;

    private SerDeserializerRegistry serDeserializerRegistry;

    private JsonbSerDeserializer serDeserializer;

    @BeforeEach
    public void setup() {
        typeRegistry = TestUtils.createSerializedDataTypeRegistry();
        jsonbConfig = TestUtils.createJsonbConfig();
        jsonbProvider = new JsonbProvider(jsonbConfig);
        serDeserializer = TestUtils.createSerDeserializer(jsonbProvider, typeRegistry);
        serDeserializerRegistry = TestUtils.createSerDeserializerRegistry(serDeserializer);
        TestUtils.register(jsonbConfig, serDeserializerRegistry, serDeserializerRegistry);
    }

    @AfterEach
    public void teardown() {
        typeRegistry = null;
        jsonbProvider = null;
        serDeserializer = null;
        jsonbConfig = null;
    }

    public SerializedDataTypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    public JsonbConfig getJsonbConfig() {
        return jsonbConfig;
    }

    public JsonbProvider getJsonbProvider() {
        return jsonbProvider;
    }

    public SerDeserializerRegistry getSerDeserializerRegistry() {
        return serDeserializerRegistry;
    }

    public JsonbSerDeserializer getSerDeserializer() {
        return serDeserializer;
    }

}
