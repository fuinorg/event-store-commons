/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.spi;

import jakarta.activation.MimeTypeParseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link SimpleSerializerDeserializerRegistry} class.
 */
// CHECKSTYLE:OFF Test
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
        final JsonDeSerializer deserializer = new JsonDeSerializer();
        final EnhancedMimeType mimeType = EnhancedMimeType.create(
                "application", "json", Charset.forName("utf-8"), "1");

        // TEST
        testee.addDeserializer(type, contentType, deserializer);

        // VERIFY
        assertThat(testee.getDeserializer(type, mimeType)).isSameAs(
                deserializer);

    }

    @Test
    public void testSetGetDefaultContentType() {

        // PREPARE
        final SerializedDataType type = new SerializedDataType("MyType");
        final String contentType = "application/json";
        final JsonDeSerializer deserializer = new JsonDeSerializer();
        final EnhancedMimeType mimeType = EnhancedMimeType.create(
                "application", "json", Charset.forName("utf-8"));
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
        final JsonDeSerializer serializer = new JsonDeSerializer();

        // TEST
        testee.addSerializer(type, serializer);

        // VERIFY
        assertThat(testee.getSerializer(type)).isSameAs(serializer);

    }

}
// CHECKSTYLE:ON
