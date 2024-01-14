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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerializedDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link EscMetaJsonDeSerializer} class.
 */
// CHECKSTYLE:OFF Test
public class EscMetaJsonDeSerializerTest {

    private EscMetaJsonDeSerializer testee;

    @BeforeEach
    public void setup() throws MimeTypeParseException {
        testee = new EscMetaJsonDeSerializer();
    }

    @AfterEach
    public void teardown() {
        testee = null;
    }

    @Test
    public void testMarshalUnmarshal() {

        // PREPARE
        final SerializedDataType dataType = new SerializedDataType("MyData");
        EnhancedMimeType dataContentType = EnhancedMimeType.create("application", "xml");
        final SerializedDataType metaType = new SerializedDataType("MyMeta");
        EnhancedMimeType metaContentType = EnhancedMimeType.create("application", "json");
        final JsonObject meta = Json.createObjectBuilder().add("user", "peter").add("ip", "127.0.0.1").build();
        final EscMeta escMeta = new EscMeta(dataType.asBaseType(), dataContentType, metaType.asBaseType(), metaContentType, meta);

        // TEST
        final byte[] data = testee.marshal(escMeta, metaType);
        final EscMeta copy = testee.unmarshal(data, dataType, EnhancedMimeType.create("application/json; encoding=utf-8"));

        // VERIFY
        assertThat(copy.getDataType()).isEqualTo(dataType.asBaseType());
        assertThat(copy.getDataContentType()).isEqualTo(dataContentType);
        assertThat(copy.getMetaType()).isEqualTo(metaType.asBaseType());
        assertThat(copy.getMetaContentType()).isEqualTo(metaContentType);
        assertThat(copy.getMeta()).isInstanceOf(JsonObject.class);
        final JsonObject jsonObj = (JsonObject) copy.getMeta();
        assertThat(jsonObj.keySet()).contains("user", "ip");
        assertThat(jsonObj.getString("user")).isEqualTo("peter");
        assertThat(jsonObj.getString("ip")).isEqualTo("127.0.0.1");

    }
    
}
