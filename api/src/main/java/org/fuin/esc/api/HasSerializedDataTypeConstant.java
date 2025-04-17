package org.fuin.esc.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A class that has a public static constant of type {@link SerializedDataType}.
 * The expected default name of the constant is <b>SER_TYPE</b>.
 */
@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {HasSerializedDataTypeConstantValidator.class})
public @interface HasSerializedDataTypeConstant {

    /**
     * Returns the name of a public static constant in the annotated class.
     *
     * @return Name of the public static constant.
     */
    String value() default "SER_TYPE";

    String message() default "Does not define a public static constant with the given name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
