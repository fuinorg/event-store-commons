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

import java.nio.charset.Charset;

import javax.activation.MimeTypeParseException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link TextDeSerializer} class.
 */
// CHECKSTYLE:OFF Test
public class TextDeSerializerTest {

    private static final Charset CHARSET = Charset.forName("iso-8859-1");

    private TextDeSerializer testee;

    @Before
    public void setup() throws MimeTypeParseException {
        testee = new TextDeSerializer(CHARSET);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testMarshalUnmarshal() {

        // PREPARE
        final String original = "Whatever";

        // TEST
        final byte[] data = testee.marshal(original);
        final String copy = testee
                .unmarshal(data, EnhancedMimeType.create("text/plain; encoding=" + CHARSET));

        // VERIFY
        assertThat(copy).isEqualTo(original);

    }

    @Test
    public void testUnmarshalString() {

        // PREPARE
        final String original = "Whatever";

        // TEST
        final String copy = testee.unmarshal(original,
                EnhancedMimeType.create("text/plain; encoding=" + CHARSET));

        // VERIFY
        assertThat(copy).isEqualTo(original);

    }

    @Test
    public void testGetMimeType() {

        // TEST & VERIFY
        assertThat(testee.getMimeType()).isEqualTo(EnhancedMimeType.create("text", "plain", CHARSET));
        assertThat(new TextDeSerializer().getMimeType()).isEqualTo(
                EnhancedMimeType.create("text", "plain", Charset.forName("utf-8")));

    }

}
// CHECKSTYLE:ON
