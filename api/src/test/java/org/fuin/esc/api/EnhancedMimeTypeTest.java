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

import jakarta.activation.MimeTypeParameterList;
import jakarta.activation.MimeTypeParseException;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link EnhancedMimeType} class.
 */
public class EnhancedMimeTypeTest {

    private EnhancedMimeType testee;

    @BeforeEach
    public void setup() throws MimeTypeParseException {
        testee = new EnhancedMimeType(
                "application/xml;version=1.0.2;encoding=utf-8");
    }

    @AfterEach
    public void teardown() {
        testee = null;
    }

    @Test
    public void testConstrcutionPrimarySub() throws MimeTypeParseException {

        // PREPARE & TEST
        final EnhancedMimeType testee2 = new EnhancedMimeType("application",
                "json");

        // VERIFY
        assertThat(testee2.getPrimaryType()).isEqualTo("application");
        assertThat(testee2.getSubType()).isEqualTo("json");
        assertThat(testee2.getVersion()).isNull();
        assertThat(testee2.getEncoding()).isNull();
    }

    @Test
    public void testConstrcutionPrimarySubEncoding()
            throws MimeTypeParseException {

        // PREPARE & TEST
        final EnhancedMimeType testee = new EnhancedMimeType("application",
                "json", StandardCharsets.UTF_8);

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
    public void testEqualsHashCode() throws MimeTypeParseException {
        final MimeTypeParameterList listA = new MimeTypeParameterList(";charset=utf-8");
        final MimeTypeParameterList listB = new MimeTypeParameterList(";charset=iso-8859-2");
        EqualsVerifier.forClass(EnhancedMimeType.class)
                .suppress(Warning.NULL_FIELDS, Warning.NONFINAL_FIELDS)
                .withPrefabValues(MimeTypeParameterList.class, listA, listB)
                .verify();
    }

}
