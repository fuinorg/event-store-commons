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

import org.fuin.esc.spi.Base64Data;
import org.fuin.esc.spi.DataWrapper;
import org.junit.Test;

/**
 * Test for {@link DataWrapper} class.
 */
public class DataWrapperTest {

    @Test
    public final void testMarshal() throws Exception {

        // PREPARE
        final Base64Data base64 = new Base64Data("Hello world!".getBytes(Charset.forName("utf-8")));
        final DataWrapper testee = new DataWrapper(base64);

        // TEST
        final String result = marshal(testee, DataWrapper.class, Base64Data.class);

        // VERIFY
        assertThat(result).isEqualTo(
                XML_PREFIX + "<Wrapper><Base64>SGVsbG8gd29ybGQh</Base64></Wrapper>");

    }

    @Test
    public final void testUnmarshal() throws Exception {

        // TEST
        final DataWrapper testee = unmarshal(XML_PREFIX
                + "<Wrapper><Base64>SGVsbG8gd29ybGQh</Base64></Wrapper>", DataWrapper.class,
                Base64Data.class);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getObj()).isInstanceOf(Base64Data.class);

    }

}
