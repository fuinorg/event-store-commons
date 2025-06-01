package org.fuin.esc.api;

import org.fuin.objects4j.common.TypeConstantValidator;
import org.fuin.utils4j.TestOmitted;

/**
 * Determines if the annotated class has a public static constant with the given name and {@link SerializedDataType} type.
 */
@TestOmitted("Functionality tested with base class")
public final class HasSerializedDataTypeConstantValidator extends TypeConstantValidator<HasSerializedDataTypeConstant> {

    public HasSerializedDataTypeConstantValidator() {
        super(HasSerializedDataTypeConstant.class, "value", null, null, SerializedDataType.class);
    }

}
