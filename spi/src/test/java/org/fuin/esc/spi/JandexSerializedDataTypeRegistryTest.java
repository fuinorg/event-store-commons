package org.fuin.esc.spi;

import org.fuin.esc.api.JandexSerializedDataTypeRegistry;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link JandexSerializedDataTypeRegistry} class.
 */
public class JandexSerializedDataTypeRegistryTest {

    @Test
    public void testBaseDataTypes() {
        final JandexSerializedDataTypeRegistry testee = new JandexSerializedDataTypeRegistry();
        assertThat(testee.getClasses()).containsOnly(
                Base64Data.class,
                EscEvent.class,
                EscEvents.class,
                EscMeta.class
        );
    }

}
