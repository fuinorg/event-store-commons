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

import jakarta.activation.MimeTypeParseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link SimpleSerializerDeserializerRegistry} class.
 */
public class SimpleSerializerDeserializerRegistryTest {

    private SimpleSerializerDeserializerRegistry testee;

    @BeforeEach
    public void setup() throws MimeTypeParseException {
        testee = new SimpleSerializerDeserializerRegistry();
    }

    @AfterEach
    public void teardown() {
        testee = null;
    }

    @Test
    public void testAddGetDeserializer() {

        // PREPARE
        final SerializedDataType type = new SerializedDataType("MyType");
        final String contentType = "application/json";
        final Deserializer deserializer = Mockito.mock(Deserializer.class);
        final EnhancedMimeType mimeType = EnhancedMimeType.create(
                "application", "json", StandardCharsets.UTF_8, "1");

        // TEST
        testee.addDeserializer(type, contentType, deserializer);

        // VERIFY
        assertThat(testee.getDeserializer(type, mimeType)).isSameAs(deserializer);

    }

    @Test
    public void testSetGetDefaultContentType() {

        // PREPARE
        final SerializedDataType type = new SerializedDataType("MyType");
        final String contentType = "application/json";
        final Deserializer deserializer = Mockito.mock(Deserializer.class);
        final EnhancedMimeType mimeType = EnhancedMimeType.create(
                "application", "json", StandardCharsets.UTF_8);
        testee.addDeserializer(type, contentType, deserializer);

        // TEST
        testee.setDefaultContentType(type, mimeType);

        // VERIFY
        assertThat(testee.getDeserializer(type)).isSameAs(deserializer);
        assertThat(testee.getDefaultContentType(type)).isSameAs(mimeType);

    }

    @Test
    public void testAddGetSerializer() {

        // PREPARE
        final SerializedDataType type = new SerializedDataType("MyType");
        final Serializer serializer = Mockito.mock(Serializer.class);

        // TEST
        testee.addSerializer(type, serializer);

        // VERIFY
        assertThat(testee.getSerializer(type)).isSameAs(serializer);

    }

}

