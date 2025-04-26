package org.fuin.esc.jackson;

import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.objects4j.jackson.ImmutableObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.fuin.esc.jackson.TestUtils.createSerDeserializerRegistry;

/**
 * Base test that inigtializes registry and mapper.
 */
public abstract class AbstractTest {

    private SerializedDataTypeRegistry typeRegistry;

    private ImmutableObjectMapper.Builder mapperBuilder;

    private ImmutableObjectMapper.Provider mapperProvider;

    private SerDeserializerRegistry serDeserializerRegistry;

    private JacksonSerDeserializer serDeserializer;

    @BeforeEach
    public void setup() {
        typeRegistry = TestUtils.createSerializedDataTypeRegistry();
        mapperBuilder = TestUtils.createMapperBuilder();
        mapperProvider = new ImmutableObjectMapper.Provider(mapperBuilder);
        serDeserializer = TestUtils.createSerDeserializer(mapperProvider, typeRegistry);
        serDeserializerRegistry = createSerDeserializerRegistry(serDeserializer);
        mapperBuilder.registerModule(new EscJacksonAdapterModule(serDeserializerRegistry, serDeserializerRegistry));
    }

    @AfterEach
    public void teardown() {
        typeRegistry = null;
        mapperProvider = null;
        serDeserializer = null;
        mapperBuilder = null;
    }

    public SerializedDataTypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    public ImmutableObjectMapper.Builder getMapperBuilder() {
        return mapperBuilder;
    }

    public ImmutableObjectMapper.Provider getMapperProvider() {
        return mapperProvider;
    }

    public SerDeserializerRegistry getSerDeserializerRegistry() {
        return serDeserializerRegistry;
    }

    public JacksonSerDeserializer getSerDeserializer() {
        return serDeserializer;
    }

}
