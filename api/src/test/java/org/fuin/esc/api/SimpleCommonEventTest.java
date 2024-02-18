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

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link SimpleCommonEvent} class.
 */
public class SimpleCommonEventTest {

    private static final EventId ID = new EventId();

    private static final TypeName DATA_TYPE = new TypeName("MyEvent");

    private static final MyEvent DATA = new MyEvent("Peter");

    private static final TypeName META_TYPE = new TypeName("MyMeta");

    private static final String META = " { \"ip\" : \"127.0.0.1\" }";

    private SimpleCommonEvent testee;

    @BeforeEach
    public void setup() {
        testee = new SimpleCommonEvent(ID, DATA_TYPE, DATA, META_TYPE, META);
    }

    @AfterEach
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

