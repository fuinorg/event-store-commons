package org.fuin.esc.spi;

import static org.fest.assertions.Assertions.assertThat;

import javax.activation.MimeTypeParseException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link TypeVersion} class.
 */
// CHECKSTYLE:OFF Test
public class TypeVersionTest {

    private static final String VERSION = "1";
    private static final String TYPE = "ItemAddedEvent";
    private TypeVersion testee;

    @Before
    public void setup() throws MimeTypeParseException {
        testee = new TypeVersion(TYPE, VERSION);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testGetter() {
        assertThat(testee.getType()).isEqualTo(TYPE);
        assertThat(testee.getVersion()).isEqualTo(VERSION);
    }

}
