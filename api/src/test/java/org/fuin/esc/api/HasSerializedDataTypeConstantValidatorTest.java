/**
 * Copyright (C) 2013 Future Invent Informationsmanagement GmbH. All rights
 * reserved. <http://www.fuin.org/>
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class HasSerializedDataTypeConstantValidatorTest {

    public static final String FIELD_NAME = "SER_TYPE";
    private static Validator validator;

    @BeforeAll
    static void beforeAll() {
        try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    public final void testValid() {
        assertThat(validator.validate(new MyClassValid())).isEmpty();
        assertThat(HasSerializedDataTypeConstantValidator
                .extractValue(MyClassValid.class, FIELD_NAME)).isEqualTo(new SerializedDataType("XYZ"));
    }

    @Test
    public final void testNotStatic() {

        assertThat(first(validator.validate(new MyClassNotStatic()))).contains("#1");

        assertThatThrownBy(
                () -> HasSerializedDataTypeConstantValidator.extractValue(MyClassNotStatic.class, FIELD_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#1");

    }

    @Test
    public final void testNotPublic() {

        assertThat(first(validator.validate(new MyClassNotPublic()))).contains("#2");

        assertThatThrownBy(
                () -> HasSerializedDataTypeConstantValidator.extractValue(MyClassNotPublic.class, FIELD_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#2");

    }

    @Test
    public final void testWrongReturnType() {

        assertThat(first(validator.validate(new MyClassWrongType()))).contains("#3");

        assertThatThrownBy(
                () -> HasSerializedDataTypeConstantValidator.extractValue(MyClassWrongType.class, FIELD_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#3");

    }

    @Test
    public final void testWrongReturn() {

        assertThat(first(validator.validate(new MyClassNullValue()))).contains("#4");

        assertThatThrownBy(
                () -> HasSerializedDataTypeConstantValidator.extractValue(MyClassNullValue.class, FIELD_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#4");

    }

    @Test
    public final void testNoField() {

        assertThat(first(validator.validate(new MyClassNoField()))).contains("#2");

        assertThatThrownBy(
                () -> HasSerializedDataTypeConstantValidator.extractValue(MyClassNoField.class, FIELD_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#2");

    }

    @Test
    public final void testNotFinal() {

        assertThat(first(validator.validate(new MyClassNotFinal()))).contains("#5");

        assertThatThrownBy(
                () -> HasSerializedDataTypeConstantValidator.extractValue(MyClassNotFinal.class, FIELD_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#5");

    }

    private static String first(Set<?> violations) {
        return violations.stream().map(v -> ((ConstraintViolation<?>) v).getMessage()).findFirst().orElse(null);
    }

    @HasSerializedDataTypeConstant
    public static final class MyClassValid {
        public static final SerializedDataType SER_TYPE = new SerializedDataType("XYZ");
    }

    @HasSerializedDataTypeConstant
    @SuppressWarnings("java:S116") // Intentionally wrong naming for test
    public static final class MyClassNotStatic {
        public final SerializedDataType SER_TYPE = new SerializedDataType("XYZ");
    }

    @HasSerializedDataTypeConstant
    public static final class MyClassNotPublic {
        protected static final SerializedDataType SER_TYPE = new SerializedDataType("XYZ");
    }

    @HasSerializedDataTypeConstant
    public static final class MyClassNoField {
    }

    @HasSerializedDataTypeConstant
    public static final class MyClassWrongType {
        public static final Integer SER_TYPE = 123;
    }

    @HasSerializedDataTypeConstant
    public static final class MyClassNullValue {
        public static final SerializedDataType SER_TYPE = null;
    }

    @HasSerializedDataTypeConstant
    @SuppressWarnings("java:S3008") // Intentionally wrong naming for test
    public static final class MyClassNotFinal {
        public static SerializedDataType SER_TYPE = new SerializedDataType("XYZ");
    }


}
