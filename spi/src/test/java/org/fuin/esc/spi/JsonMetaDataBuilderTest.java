/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.spi;

import static org.fest.assertions.Assertions.assertThat;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.fuin.esc.spi.JsonMetaDataBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JsonMetaDataBuilder} class.
 */
// CHECKSTYLE:OFF Test
public class JsonMetaDataBuilderTest {

    private JsonMetaDataBuilder testee;

    @Before
    public void setup() {
        testee = new JsonMetaDataBuilder();
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testAdd() {

        // PREPARE
        final String contentType = "application/json; encoding=utf-8; version=1";
        final JsonObjectBuilder sub = Json.createObjectBuilder().add("z", true);
        final JsonArrayBuilder array = Json.createArrayBuilder().add(true)
                .add(123).add("Peter");
        final JsonObject obj = Json.createObjectBuilder().add("a", 1)
                .add("b", "B").add("sub", sub).add("myarr", array).build();

        // TEST
        testee.init(obj);
        testee.add("content-type", contentType);
        testee.add("encryption", false);
        testee.add("c", 789);

        // VERIFY
        final JsonObject result = testee.build();
        assertThat(result.getString("content-type")).isEqualTo(contentType);
        assertThat(result.getBoolean("encryption")).isFalse();
        assertThat(result.getInt("c")).isEqualTo(789);

    }

    @Test
    public void testCopyObject() {

        // PREPARE
        final JsonObjectBuilder sub = Json.createObjectBuilder().add("z", true);
        final JsonArrayBuilder array = Json.createArrayBuilder().add(true)
                .add(123).add("Peter");
        final JsonObject obj = Json.createObjectBuilder().add("a", 1)
                .add("b", "B").add("sub", sub).add("myarr", array).build();

        // TEST
        final JsonObjectBuilder builder = JsonMetaDataBuilder.copy(obj);

        // VERIFY
        final String original = obj.toString();
        final String copy = builder.build().toString();
        assertThat(copy).isEqualTo(original);

    }

    @Test
    public void testCopyArray() {

        // PREPARE
        final JsonObjectBuilder sub = Json.createObjectBuilder().add("z", true);
        final JsonObjectBuilder obj = Json.createObjectBuilder().add("a", 1)
                .add("b", "B").add("sub", sub);
        final JsonArray arr = Json.createArrayBuilder().add(true).add(123)
                .add("Peter").add(obj).build();

        // TEST
        final JsonArrayBuilder builder = JsonMetaDataBuilder.copy(arr);

        // VERIFY
        final String original = arr.toString();
        final String copy = builder.build().toString();
        assertThat(copy).isEqualTo(original);

    }

}
