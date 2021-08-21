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
package org.fuin.esc.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link SimpleCommonEvent} class.
 */
// CHECKSTYLE:OFF Test
public class SimpleCommonEventTest {

    private static final EventId ID = new EventId();

    private static final TypeName DATA_TYPE = new TypeName("MyEvent");

    private static MyEvent DATA = new MyEvent("Peter");

    private static final TypeName META_TYPE = new TypeName("MyMeta");

    private static JsonObject META = Json.createObjectBuilder().add("ip", "127.0.0.1").build();

    private SimpleCommonEvent testee;

    @Before
    public void setup() {
        testee = new SimpleCommonEvent(ID, DATA_TYPE, DATA, META_TYPE, META);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(SimpleCommonEvent.class).suppress(Warning.ALL_FIELDS_SHOULD_BE_USED).verify();
    }

    @Test
    public void testGetter() {
        assertThat(testee.getId()).isEqualTo(ID);
        assertThat(testee.getDataType()).isEqualTo(DATA_TYPE);
        assertThat(testee.getData()).isEqualTo(DATA);
        assertThat(testee.getMetaType()).isEqualTo(META_TYPE);
        assertThat(testee.getMeta()).isEqualTo(META);
    }

}
// CHECKSTYLE:ON
