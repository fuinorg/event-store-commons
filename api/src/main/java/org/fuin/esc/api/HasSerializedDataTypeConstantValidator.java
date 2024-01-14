package org.fuin.esc.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Determines if the annotated class has a public static constant with the given name and {@link SerializedDataType} type.
 */
public class HasSerializedDataTypeConstantValidator implements ConstraintValidator<HasSerializedDataTypeConstant, Object> {

    private String name;

    @Override
    public void initialize(HasSerializedDataTypeConstant annotation) {
        this.name = annotation.value();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        try {
            final Field field = obj.getClass().getField(name);
            final int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers)) {
                error(context, "Field '" + name + "' is not static (#1)");
                return false;
            }
            if (field.getType() != SerializedDataType.class) {
                error(context, "Expected constant '" + name + "' to be of type '" + SerializedDataType.class.getName() + "', but was: " + field.getType().getName() + " (#3)");
                return false;
            }
            final Object value = field.get(obj);
            if (value == null) {
                error(context, "Constant '" + name + "' is expected to be a non-null value (#4)");
                return false;
            }
            if (!Modifier.isFinal(modifiers)) {
                error(context, "Constant '" + name + "' is not not final (#5)");
                return false;
            }
            return true;
        } catch (final NoSuchFieldException ex) {
            error(context, "The field '" + name + "' is undefined or it is not public (#2)");
            return false;
        } catch (final IllegalAccessException ex) {
            throw new IllegalStateException("Failed to execute method", ex);
        }

    }

    private void error(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

}
