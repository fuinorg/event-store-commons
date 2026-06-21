package org.fuin.esc.api;

import org.fuin.objects4j.common.ThreadSafe;

import java.util.Set;

/**
 * Returns classes that are annotated with {@link HasSerializedDataTypeConstant} and should be
 * included in the applications {@link SerializedDataTypeRegistry}.
 * All implementations are expected to be thread safe.
 */
@ThreadSafe
public interface SerializedDataTypesRegistrationRequest {

    /**
     * Returns the class to type mappings that should be registered.
     *
     * @return Set of mappings.
     */
    Set<SerializedDataType2ClassMapping> getMappingsToRegister();

}
