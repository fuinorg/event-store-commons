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
package org.fuin.esc.jpa;

import static org.fest.assertions.Assertions.assertThat;
import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link SimpleJpaStreamId} class.
 */
// CHECKSTYLE:OFF Test
public class SimpleJpaStreamIdTest {

    private static final String NAME = "MyStream1";

    private static final String TABLE_NAME = "MY_TABLE_NAME";

    private SimpleJpaStreamId testee;

    @Before
    public void setup() {
        testee = new SimpleJpaStreamId(NAME, TABLE_NAME);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(SimpleJpaStreamId.class).verify();
    }

    @Test
    public void testGetter() {
        assertThat(testee.getName()).isEqualTo(NAME);
        assertThat(testee.asString()).isEqualTo(NAME);
        assertThat(testee.isProjection()).isFalse();
        assertThat(testee.getParameters()).isEmpty();
        assertThat(testee.getNativeTableName()).isEqualTo(TABLE_NAME);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetSingleParamValue() {
        testee.getSingleParamValue();
    }
}
// CHECKSTYLE:ON
