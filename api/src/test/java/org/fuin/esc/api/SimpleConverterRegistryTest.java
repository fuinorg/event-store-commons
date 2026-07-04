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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link SimpleConverterRegistry} class.
 */
public class SimpleConverterRegistryTest {

    private static final SerializedDataType TYPE = new SerializedDataType("MyEvent");

    @Test
    public void testUpcastChainToLatest() {

        // PREPARE: v1 -> v2 -> v3
        final ConverterRegistry registry = new SimpleConverterRegistry.Builder()
                .add(TYPE, "1", "2", append("|v2"))
                .add(TYPE, "2", "3", append("|v3"))
                .build();

        // TEST & VERIFY
        assertThat((Object) registry.upcastToLatest(TYPE, "1", "data")).isEqualTo("data|v2|v3");
        assertThat((Object) registry.upcastToLatest(TYPE, "2", "data")).isEqualTo("data|v3");
        // Already latest / no outgoing converter -> unchanged
        assertThat((Object) registry.upcastToLatest(TYPE, "3", "data")).isEqualTo("data");

    }

    @Test
    public void testUpcastUnchangedWhenNoChain() {

        // PREPARE
        final ConverterRegistry registry = new SimpleConverterRegistry.Builder().build();

        // TEST & VERIFY: unknown type and null version both return the input unchanged
        assertThat((Object) registry.upcastToLatest(TYPE, "1", "data")).isEqualTo("data");
        assertThat((Object) registry.upcastToLatest(TYPE, null, "data")).isEqualTo("data");

    }

    @Test
    public void testSkipConverter() {

        // PREPARE: a single converter that jumps v1 -> v3 directly
        final ConverterRegistry registry = new SimpleConverterRegistry.Builder()
                .add(TYPE, "1", "3", append("|v3"))
                .build();

        // TEST & VERIFY
        assertThat((Object) registry.upcastToLatest(TYPE, "1", "data")).isEqualTo("data|v3");

    }

    @Test
    public void testConvertToSpecificVersion() {

        // PREPARE
        final ConverterRegistry registry = new SimpleConverterRegistry.Builder()
                .add(TYPE, "1", "2", append("|v2"))
                .add(TYPE, "2", "3", append("|v3"))
                .build();

        // TEST & VERIFY
        assertThat((Object) registry.convert(TYPE, "1", "2", "data")).isEqualTo("data|v2");
        assertThat((Object) registry.convert(TYPE, "1", "3", "data")).isEqualTo("data|v2|v3");
        // Same version -> unchanged
        assertThat((Object) registry.convert(TYPE, "2", "2", "data")).isEqualTo("data");

    }

    @Test
    public void testConvertNoPathThrows() {

        // PREPARE
        final ConverterRegistry registry = new SimpleConverterRegistry.Builder()
                .add(TYPE, "1", "2", append("|v2"))
                .build();

        // TEST & VERIFY
        assertThatThrownBy(() -> registry.convert(TYPE, "1", "9", "data"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No converter chain");
        assertThatThrownBy(() -> registry.convert(TYPE, null, "2", "data"))
                .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    public void testConverterExists() {

        // PREPARE
        final ConverterRegistry registry = new SimpleConverterRegistry.Builder()
                .add(TYPE, "1", "2", append("|v2"))
                .build();

        // TEST & VERIFY
        assertThat(registry.converterExists(TYPE, "1")).isTrue();
        assertThat(registry.converterExists(TYPE, "2")).isFalse();
        assertThat(registry.converterExists(TYPE, null)).isFalse();
        assertThat(registry.converterExists(new SerializedDataType("Other"), "1")).isFalse();

    }

    @Test
    public void testCyclicChainDetected() {

        // PREPARE: v1 -> v2 -> v1
        final ConverterRegistry registry = new SimpleConverterRegistry.Builder()
                .add(TYPE, "1", "2", append("|v2"))
                .add(TYPE, "2", "1", append("|v1"))
                .build();

        // TEST & VERIFY
        assertThatThrownBy(() -> registry.upcastToLatest(TYPE, "1", "data"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cyclic");

    }

    @Test
    public void testDuplicateSourceVersionRejected() {

        // TEST & VERIFY
        assertThatThrownBy(() -> new SimpleConverterRegistry.Builder()
                .add(TYPE, "1", "2", append("|v2"))
                .add(TYPE, "1", "3", append("|v3")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");

    }

    private static Converter<String, String> append(final String suffix) {
        return new Converter<>() {
            @Override
            public Class<String> getSourceType() {
                return String.class;
            }

            @Override
            public Class<String> getTargetType() {
                return String.class;
            }

            @Override
            public String convert(final String source) {
                return source + suffix;
            }
        };
    }

}
