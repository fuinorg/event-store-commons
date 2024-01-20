package org.fuin.esc.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        final Result result = analyze(obj.getClass(), name);
        if (result.message() == null) {
            return true;
        }
        error(context, result.message());
        return false;
    }

    private void error(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    private static Result analyze(final Class<?> clasz, final String name) {
        try {
            final Field field = clasz.getField(name);
            final int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers)) {
                return new Result("Field '" + name + "' is not static (#1)", null);
            }
            if (field.getType() != SerializedDataType.class) {
                return new Result("Expected constant '" + name + "' to be of type '" + SerializedDataType.class.getName() + "', but was: " + field.getType().getName() + " (#3)", null);
            }
            final Object value = field.get(clasz);
            if (value == null) {
                return new Result("Constant '" + name + "' is expected to be a non-null value (#4)", null);
            }
            if (!Modifier.isFinal(modifiers)) {
                return new Result("Constant '" + name + "' is not not final (#5)", null);
            }
            return new Result(null, value);
        } catch (final NoSuchFieldException ex) {
            return new Result("The field '" + name + "' is undefined or it is not public (#2)", null);
        } catch (final IllegalAccessException ex) {
            throw new IllegalStateException("Failed to execute method", ex);
        }
    }

    private record Result(String message, Object value) {
    }

    /**
     * Returns a constant of type {@link SerializedDataType} in a class. Throws an {@link IllegalArgumentException}
     * in case there is a problem with the field.
     *
     * @param clasz      Class to inspect.
     * @param fieldName Name of the public static field of type {@link SerializedDataType}.
     * @return Value of the constant.
     */
    public static SerializedDataType extractValue(final Class<?> clasz, final String fieldName) {
        final Result result = analyze(clasz, fieldName);
        if (result.message() == null) {
            return (SerializedDataType) result.value();
        }
        throw new IllegalArgumentException(result.message());
    }

}
