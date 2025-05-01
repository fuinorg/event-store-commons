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
package org.fuin.esc.jaxb;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.fuin.utils4j.jaxb.MarshallerBuilder;
import org.fuin.utils4j.jaxb.UnmarshallerBuilder;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.jaxb.JaxbUtils.XML_PREFIX;
import static org.fuin.utils4j.jaxb.JaxbUtils.marshal;
import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;

/**
 * Test for {@link Base64Data} class.
 */
public class Base64DataTest {

    @Test
    public final void testMarshal() throws Exception {

        // PREPARE
        final Base64Data testee = new Base64Data("Hello world!".getBytes(Charset.forName("utf-8")));

        // TEST
        final Marshaller marshaller = new MarshallerBuilder().addClassesToBeBound(Base64Data.class).build();
        final String result = marshal(marshaller, testee);

        // VERIFY
        assertThat(result).isEqualTo(XML_PREFIX + "<Base64>SGVsbG8gd29ybGQh</Base64>");

    }

    @Test
    public final void testUnmarshal() throws Exception {

        // TEST
        final Unmarshaller unmarshaller = new UnmarshallerBuilder().addClassesToBeBound(Base64Data.class).build();
        final Base64Data testee = unmarshal(unmarshaller, XML_PREFIX + "<Base64>SGVsbG8gd29ybGQh</Base64>");

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getEncoded()).isEqualTo("SGVsbG8gd29ybGQh");
        assertThat(testee.getDecoded()).isEqualTo(
                new byte[]{72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33});

    }

}
