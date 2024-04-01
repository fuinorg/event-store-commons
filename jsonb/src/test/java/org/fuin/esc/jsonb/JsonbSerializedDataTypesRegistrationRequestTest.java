package org.fuin.esc.jsonb;

import org.fuin.esc.api.SerializedDataTypesRegistrationRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link JsonbSerializedDataTypesRegistrationRequest} class.
 */
public class JsonbSerializedDataTypesRegistrationRequestTest {

    @Test
    void testGetService() {
        final ServiceLoader<SerializedDataTypesRegistrationRequest> loader = ServiceLoader.load(SerializedDataTypesRegistrationRequest.class);
        final Optional<SerializedDataTypesRegistrationRequest> services = StreamSupport.stream(loader.spliterator(), false).findFirst();
        assertThat(services).isNotEmpty();
        assertThat(services.get()).isInstanceOf(JsonbSerializedDataTypesRegistrationRequest.class);
    }

}
