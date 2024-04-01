package org.fuin.esc.jaxb;

import com.google.auto.service.AutoService;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IEscEvent;
import org.fuin.esc.api.IEscEvents;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerializedDataType2ClassMapping;
import org.fuin.esc.api.SerializedDataTypesRegistrationRequest;

import java.util.Set;

/**
 * Request to register the JAX-B related {@link org.fuin.esc.api.SerializedDataType} to class mappings.
 */
@AutoService(SerializedDataTypesRegistrationRequest.class)
public class JaxbSerializedDataTypesRegistrationRequest implements SerializedDataTypesRegistrationRequest {

    @Override
    public Set<SerializedDataType2ClassMapping> getMappingsToRegister() {
        return Set.of(
                new SerializedDataType2ClassMapping(IBase64Data.SER_TYPE, Base64Data.class),
                new SerializedDataType2ClassMapping(IEscEvent.SER_TYPE, EscEvent.class),
                new SerializedDataType2ClassMapping(IEscEvents.SER_TYPE, EscEvents.class),
                new SerializedDataType2ClassMapping(IEscMeta.SER_TYPE, EscMeta.class)
        );
    }

}
