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
package org.fuin.esc.intf;

import static org.fest.assertions.Assertions.assertThat;

import java.nio.charset.Charset;

import javax.activation.MimeTypeParseException;

import org.junit.Test;

//TESTCODE:BEGIN
public class VersionedMimeTypeConverterTest {

    @Test
    public final void testStringConstruction() throws MimeTypeParseException {

        // PREPARE & TEST
        final VersionedMimeType testee = new VersionedMimeType(
                "application/xml");

        // VERIFY
        assertThat(testee.getBaseType()).isEqualTo("application/xml");
        assertThat(testee.getPrimaryType()).isEqualTo("application");
        assertThat(testee.getSubType()).isEqualTo("xml");
        assertThat(testee.getEncoding()).isEqualTo(Charset.forName("utf-8"));
        assertThat(testee.getVersion()).isEqualTo("1");
        assertThat(testee.getParameters().size()).isEqualTo(0);

    }

    @Test
    public final void testStringConstructionWithParams() throws MimeTypeParseException {

        // PREPARE & TEST
        final VersionedMimeType testee = new VersionedMimeType(
                "application/xml;encoding=iso-8859-1;version=2;another=abc");

        // VERIFY
        assertThat(testee.getBaseType()).isEqualTo("application/xml");
        assertThat(testee.getPrimaryType()).isEqualTo("application");
        assertThat(testee.getSubType()).isEqualTo("xml");
        assertThat(testee.getEncoding()).isEqualTo(Charset.forName("iso-8859-1"));
        assertThat(testee.getVersion()).isEqualTo("2");
        assertThat(testee.getParameter("another")).isEqualTo("abc");
        assertThat(testee.getParameters().size()).isEqualTo(3);

    }
    
    @Test
    public final void testTypeConstruction() throws MimeTypeParseException {

        // PREPARE & TEST
        final VersionedMimeType testee = new VersionedMimeType(
                "application", "json", Charset.forName("iso-8859-1"), "2");

        // VERIFY
        assertThat(testee.getBaseType()).isEqualTo("application/json");
        assertThat(testee.getPrimaryType()).isEqualTo("application");
        assertThat(testee.getSubType()).isEqualTo("json");
        assertThat(testee.getEncoding()).isEqualTo(Charset.forName("ISO-8859-1"));
        assertThat(testee.getVersion()).isEqualTo("2");
        assertThat(testee.getParameters().size()).isEqualTo(2);

    }
    
}
// TESTCODE:END
