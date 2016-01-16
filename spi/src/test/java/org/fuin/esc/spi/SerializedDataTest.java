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

import static org.assertj.core.api.Assertions.assertThat;

import javax.activation.MimeTypeParseException;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link SerializedData} class.
 */
// CHECKSTYLE:OFF Test
public class SerializedDataTest {

    private static final SerializedDataType TYPE = new SerializedDataType(
            "MyType");

    private static final EnhancedMimeType MIME_TYPE = EnhancedMimeType
            .create("application/xml; encoding=utf-8");

    private static final byte[] RAW = new byte[] { 0x1, 0x2, 0x3 };

    private SerializedData testee;

    @Before
    public void setup() throws MimeTypeParseException {
        testee = new SerializedData(TYPE, MIME_TYPE, RAW);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testGetter() {

        assertThat(testee.getType()).isEqualTo(TYPE);
        assertThat(testee.getMimeType()).isEqualTo(MIME_TYPE);
        assertThat(testee.getRaw()).isEqualTo(RAW);

    }
    
    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(SerializedData.class).verify();
    }
    

}
// CHECKSTYLE:ON
