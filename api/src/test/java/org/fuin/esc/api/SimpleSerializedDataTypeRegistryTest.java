/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the {@link SimpleSerializedDataTypeRegistry} class.
 */
public class SimpleSerializedDataTypeRegistryTest {

    @Test
    public void testFindClass() {

        final SimpleSerializedDataTypeRegistry testee = new SimpleSerializedDataTypeRegistry();

        final SerializedDataType stringType = new SerializedDataType("String");
        testee.add(stringType, String.class);

        final SerializedDataType integerType = new SerializedDataType("Integer");
        testee.add(new SerializedDataType2ClassMapping(integerType, Integer.class));

        assertThat(testee.findClass(stringType)).isEqualTo(String.class);
        assertThat(testee.findClass(integerType)).isEqualTo(Integer.class);

        try {
            testee.findClass(new SerializedDataType("NotExists"));
            fail();
        } catch (final IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("No class found for: NotExists");
        }

    }
}

