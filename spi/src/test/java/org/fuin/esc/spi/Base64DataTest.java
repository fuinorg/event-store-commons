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
import static org.fuin.utils4j.jaxb.JaxbUtils.XML_PREFIX;
import static org.fuin.utils4j.jaxb.JaxbUtils.marshal;
import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;

import java.nio.charset.Charset;

import org.junit.Test;

/**
 * Test for {@link Base64Data} class.
 */
public class Base64DataTest {

    @Test
    public final void testMarshal() throws Exception {

        // PREPARE
        final Base64Data testee = new Base64Data("Hello world!".getBytes(Charset.forName("utf-8")));

        // TEST
        final String result = marshal(testee, Base64Data.class);

        // VERIFY
        assertThat(result).isEqualTo(XML_PREFIX + "<Base64>SGVsbG8gd29ybGQh</Base64>");

    }

    @Test
    public final void testUnmarshal() throws Exception {

        // TEST
        final Base64Data testee = unmarshal(XML_PREFIX + "<Base64>SGVsbG8gd29ybGQh</Base64>",
                Base64Data.class);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getEncoded()).isEqualTo("SGVsbG8gd29ybGQh");
        assertThat(testee.getDecoded()).isEqualTo(
                new byte[] { 72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33 });

    }

}
