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

import static org.assertj.core.api.Assertions.assertThat;

import javax.activation.MimeTypeParseException;
import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JsonDeSerializer} class.
 */
// CHECKSTYLE:OFF Test
public class JsonDeSerializerTest {

    private JsonDeSerializer testee;

    @Before
    public void setup() throws MimeTypeParseException {
        testee = new JsonDeSerializer();
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testMarshalUnmarshal() {

        // PREPARE
        final JsonObject original = Json.createObjectBuilder()
                .add("name", "Peter").add("age", 21).build();

        // TEST
        final byte[] data = testee.marshal(original);
        final JsonObject copy = testee.unmarshal(data,
                EnhancedMimeType.create("application/json; encoding=utf-8"));

        // VERIFY
        assertThat(copy.keySet()).contains("name", "age");
        assertThat(copy.getString("name")).isEqualTo("Peter");
        assertThat(copy.getInt("age")).isEqualTo(21);

    }

}
// CHECKSTYLE:ON
