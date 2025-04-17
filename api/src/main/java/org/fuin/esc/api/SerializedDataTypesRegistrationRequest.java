package org.fuin.esc.api;

import java.util.Set;

/**
 * Returns classes that are annotated with {@link HasSerializedDataTypeConstant} and should be
 * included in the applications {@link SerializedDataTypeRegistry}.
 */
public interface SerializedDataTypesRegistrationRequest {

    /**
     * Returns the class to type mappings that should be registered.
     *
     * @return Set of mappings.
     */
    Set<SerializedDataType2ClassMapping> getMappingsToRegister();

}
