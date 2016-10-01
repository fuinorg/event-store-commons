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
package org.fuin.esc.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.Test;

/**
 * Tests the {@link JpaProjection} class.
 */
// CHECKSTYLE:OFF Test
public class JpaProjectionTest {

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(JpaProjection.class).suppress(Warning.STRICT_INHERITANCE)
                .suppress(Warning.ALL_FIELDS_SHOULD_BE_USED).verify();
    }

    @Test
    public void testConstructorAll() {

        // PREPARE
        final String name = "MyProjection";

        // TEST
        final JpaProjection testee = new JpaProjection(name, true);

        // VERIFY
        assertThat(testee.isEnabled()).isTrue();
        assertThat(testee.toString()).isEqualTo(name);

    }

    @Test
    public void testConstructorName() {

        // PREPARE
        final String name = "MyProjection";

        // TEST
        final JpaProjection testee = new JpaProjection(name);

        // VERIFY
        assertThat(testee.isEnabled()).isFalse();
        assertThat(testee.toString()).isEqualTo(name);

    }

}
// CHECKSTYLE:ON
