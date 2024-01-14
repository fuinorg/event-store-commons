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

public final class HasSerializedDataTypeConstantValidatorTest {

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
    }

    @Test
    public final void testNotStatic() {
        assertThat(first(validator.validate(new MyClassNotStatic()))).contains("#1");
    }

    @Test
    public final void testNotPublic() {
        assertThat(first(validator.validate(new MyClassNotPublic()))).contains("#2");
    }

    @Test
    public final void testWrongReturnType() {
        assertThat(first(validator.validate(new MyClassWrongType()))).contains("#3");
    }

    @Test
    public final void testWrongReturn() {
        assertThat(first(validator.validate(new MyClassNullValue()))).contains("#4");
    }

    @Test
    public final void testNoMethod() {
        assertThat(first(validator.validate(new MyClassNoField()))).contains("#2");
    }

    @Test
    public final void testNotFinal() {
        assertThat(first(validator.validate(new MyClassNotFinal()))).contains("#5");
    }

    private static String first(Set<?> violations) {
        return violations.stream().map(v -> ((ConstraintViolation<?>) v).getMessage()).findFirst().orElse(null);
    }

    @HasSerializedDataTypeConstant
    public static final class MyClassValid {
        public static final SerializedDataType SER_TYPE = new SerializedDataType("XYZ");
    }

    @HasSerializedDataTypeConstant
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
    public static final class MyClassNotFinal {
        public static SerializedDataType SER_TYPE = new SerializedDataType("XYZ");
    }


}
