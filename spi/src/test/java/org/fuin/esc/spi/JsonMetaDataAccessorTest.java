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

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JsonMetaDataAccessor} class.
 */
// CHECKSTYLE:OFF Test
public class JsonMetaDataAccessorTest {

    private JsonMetaDataAccessor testee;

    @Before
    public void setup() {
        testee = new JsonMetaDataAccessor();
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testGetString() {

        // PREPARE
        final JsonObject obj = Json.createObjectBuilder().add("name", "Peter")
                .build();
        testee.init(obj);

        // TEST
        final String name = testee.getString("name");
        final String notExists = testee.getString("whatever");
        final String nullArg = testee.getString(null);

        // VERIFY
        assertThat(name).isEqualTo("Peter");
        assertThat(notExists).isNull();
        assertThat(nullArg).isNull();

    }

    @Test
    public void testGetInteger() {

        // PREPARE
        final JsonObject obj = Json.createObjectBuilder().add("age", 21)
                .build();
        testee.init(obj);

        // TEST
        final Integer age = testee.getInteger("age");
        final Integer notExists = testee.getInteger("whatever");
        final Integer nullArg = testee.getInteger(null);

        // VERIFY
        assertThat(age).isEqualTo(21);
        assertThat(notExists).isNull();
        assertThat(nullArg).isNull();

    }

    @Test
    public void testGetBoolean() {

        // PREPARE
        final JsonObject obj = Json.createObjectBuilder().add("admin", true)
                .build();
        testee.init(obj);

        // TEST
        final Boolean admin = testee.getBoolean("admin");
        final Boolean notExists = testee.getBoolean("whatever");
        final Boolean nullArg = testee.getBoolean(null);

        // VERIFY
        assertThat(admin).isTrue();
        assertThat(notExists).isNull();
        assertThat(nullArg).isNull();

    }

}
// CHECKSTYLE:ON
