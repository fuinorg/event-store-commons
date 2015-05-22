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

import javax.activation.MimeTypeParseException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link TypeVersion} class.
 */
// CHECKSTYLE:OFF Test
public class TypeVersionTest {

    private static final String VERSION = "1";
    private static final String TYPE = "ItemAddedEvent";
    private TypeVersion testee;

    @Before
    public void setup() throws MimeTypeParseException {
        testee = new TypeVersion(TYPE, VERSION);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testGetter() {
        assertThat(testee.getType()).isEqualTo(TYPE);
        assertThat(testee.getVersion()).isEqualTo(VERSION);
    }

}
