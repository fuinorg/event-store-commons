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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimeTypeParseException;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link EnhancedMimeType} class.
 */
// CHECKSTYLE:OFF Test
public class EnhancedMimeTypeTest {

    private EnhancedMimeType testee;

    @Before
    public void setup() throws MimeTypeParseException {
        testee = new EnhancedMimeType(
                "application/xml;version=1.0.2;encoding=utf-8");
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testConstrcutionPrimarySub() throws MimeTypeParseException {

        // PREPARE & TEST
        final EnhancedMimeType testee = new EnhancedMimeType("application",
                "json");

        // VERIFY
        assertThat(testee.getPrimaryType()).isEqualTo("application");
        assertThat(testee.getSubType()).isEqualTo("json");
        assertThat(testee.getVersion()).isNull();
        assertThat(testee.getEncoding()).isNull();
    }

    @Test
    public void testConstrcutionPrimarySubEncoding()
            throws MimeTypeParseException {

        // PREPARE & TEST
        final EnhancedMimeType testee = new EnhancedMimeType("application",
                "json", Charset.forName("utf-8"));

        // VERIFY
        assertThat(testee.getPrimaryType()).isEqualTo("application");
        assertThat(testee.getSubType()).isEqualTo("json");
        assertThat(testee.getVersion()).isNull();
        assertThat(testee.getEncoding()).isEqualTo(Charset.forName("UTF-8"));
    }

    @Test
    public void testConstrcutionAllArgs() throws MimeTypeParseException {

        // PREPARE & TEST
        final Map<String, String> params = new HashMap<String, String>();
        params.put("a", "1");
        final EnhancedMimeType testee = new EnhancedMimeType("application",
                "json", Charset.forName("utf-8"), "1.0.2", params);

        // VERIFY
        assertThat(testee.getPrimaryType()).isEqualTo("application");
        assertThat(testee.getSubType()).isEqualTo("json");
        assertThat(testee.getEncoding()).isEqualTo(Charset.forName("utf-8"));
        assertThat(testee.getParameter(EnhancedMimeType.ENCODING)).isEqualTo(
                "UTF-8");
        assertThat(testee.getVersion()).isEqualTo("1.0.2");
        assertThat(testee.getParameter(EnhancedMimeType.VERSION)).isEqualTo(
                "1.0.2");
        assertThat(testee.getParameter("a")).isEqualTo("1");
        assertThat(testee.getParameters().size()).isEqualTo(3);
    }

    @Test
    public void testGetter() {
        assertThat(testee.getPrimaryType()).isEqualTo("application");
        assertThat(testee.getSubType()).isEqualTo("xml");
        assertThat(testee.getVersion()).isEqualTo("1.0.2");
        assertThat(testee.getEncoding()).isEqualTo(Charset.forName("utf-8"));
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(EnhancedMimeType.class)
                .suppress(Warning.NULL_FIELDS, Warning.NONFINAL_FIELDS)
                .verify();
    }

}
