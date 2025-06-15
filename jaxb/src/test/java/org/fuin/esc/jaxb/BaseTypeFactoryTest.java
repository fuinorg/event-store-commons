package org.fuin.esc.jaxb;

import jakarta.activation.MimeTypeParseException;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IBaseTypeFactory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link BaseTypeFactory} class.
 */
public class BaseTypeFactoryTest {

    @Test
    void testCreate() throws MimeTypeParseException {
        final IBaseTypeFactory testee = new BaseTypeFactory();
        assertThat(testee.createBase64Data("Hello".getBytes(StandardCharsets.UTF_8))).isInstanceOf(Base64Data.class);
        assertThat(testee.createEscMeta(
                "EventX",
                new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1"),
                "MetaY",
                new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1"),
                "Meta",
                null)).isInstanceOf(EscMeta.class);
    }

}
