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
 * Request to generate a class that implements {@link SerializedDataTypesRegistrationRequest}.
 * Should only be placed on an interface as it will be used for the implementation with "implements".
 */
@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateSerializedDataTypesRegistrationRequest {

    /**
     * Name of the class to generate. In case it's not set, the class will be named
     * like annotated interface with an "Impl" extension.
     *
     * @return Simple class name.
     */
    String name() default "";

}
