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
package org.fuin.esc.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.JaxbUtils.XML_PREFIX;
import static org.fuin.utils4j.JaxbUtils.marshal;
import static org.fuin.utils4j.JaxbUtils.unmarshal;

import java.nio.charset.Charset;

import org.junit.Test;

/**
 * Test for {@link EscUserMeta} class.
 */
public class EscUserMetaTest {

    @Test
    public final void testMarshal() throws Exception {

        // PREPARE
        final Base64Data base64 = new Base64Data("Hello world!".getBytes(Charset.forName("utf-8")));
        final EscUserMeta testee = new EscUserMeta(base64);

        // TEST
        final String result = marshal(testee, EscUserMeta.class, Base64Data.class);

        // VERIFY
        assertThat(result).isEqualTo(
                XML_PREFIX + "<EscUserMeta><Base64>SGVsbG8gd29ybGQh</Base64></EscUserMeta>");

    }

    @Test
    public final void testUnmarshal() throws Exception {

        // TEST
        final EscUserMeta testee = unmarshal(XML_PREFIX
                + "<EscUserMeta><Base64>SGVsbG8gd29ybGQh</Base64></EscUserMeta>", EscUserMeta.class,
                Base64Data.class);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getMeta()).isInstanceOf(Base64Data.class);

    }

}
