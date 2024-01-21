package org.fuin.esc.api;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link JandexSerializedDataTypeRegistry} class.
 */
public class JandexSerializedDataTypeRegistryTest {

    @Test
    public void testCreate() {
        final JandexSerializedDataTypeRegistry testee = new JandexSerializedDataTypeRegistry(new File("target/test-classes"));
        assertThat(testee.getClasses()).containsOnly(MyEvent.class);
    }

    @Test
    public void testFind() {
        final JandexSerializedDataTypeRegistry testee = new JandexSerializedDataTypeRegistry(new File("target/test-classes"));
        assertThat(testee.findClass(MyEvent.SER_TYPE)).isEqualTo(MyEvent.class);
    }

}
