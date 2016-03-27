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
import static org.fuin.utils4j.JaxbUtils.marshal;
import static org.fuin.utils4j.JaxbUtils.unmarshal;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

/**
 * Test for {@link EscEvent} class.
 */
public class EscEventTest {

    private static final String CONTENT_TYPE = "application/xml; version=1; encoding=utf-8";

    @Test
    public final void testUnMarshal() throws Exception {

        // PREPARE
        final String expectedXml = IOUtils.toString(this.getClass().getResourceAsStream(
                "/event-xml-xml-xml.xml"));

        // TEST
        final EscEvent testee = unmarshal(expectedXml, EscEvent.class, MyMeta.class, MyEvent.class,
                Base64Data.class);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getEventId()).isEqualTo("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        assertThat(testee.getEventType()).isEqualTo("MyEvent");
        assertThat(testee.getData()).isNotNull();
        assertThat(testee.getData().getObj()).isInstanceOf(MyEvent.class);
        assertThat(testee.getMeta()).isNotNull();
        assertThat(testee.getMeta().getUserMeta()).isNotNull();
        assertThat(testee.getMeta().getUserMeta().getObj()).isInstanceOf(MyMeta.class);

        // TEST
        final String xml = marshal(testee, EscEvent.class, MyMeta.class, MyEvent.class, Base64Data.class);

        // VERIFY
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(expectedXml, xml);

    }

}
