/**
 * Copyright (C) 2013 Future Invent Informationsmanagement GmbH. All rights
 * reserved. <http://www.fuin.org/>
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
package org.fuin.esc.api;

import static org.fest.assertions.Assertions.assertThat;

import javax.json.Json;
import javax.json.JsonObject;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link CommonEvent} class.
 */
// CHECKSTYLE:OFF Test
public class CommonEventTest {

    private static final String ID = "5741bcf1-9292-446b-84c1-957ed53b8d88";

    private static final String TYPE = "MyEvent";
    
    private static MyEvent DATA = new MyEvent("Peter");

    private static JsonObject META = Json.createObjectBuilder()
            .add("ip", "127.0.0.1").build();

    private CommonEvent testee;

    @Before
    public void setup() {
        testee = new CommonEvent(ID, TYPE, DATA, META);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(CommonEvent.class).verify();
    }

    @Test
    public void testGetter() {
        assertThat(testee.getId()).isEqualTo(ID);
        assertThat(testee.getData()).isEqualTo(DATA);
        assertThat(testee.getMeta()).isEqualTo(META);
    }

}
// CHECKSTYLE:ON
