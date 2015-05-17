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
package org.fuin.esc.test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fuin.units4j.Units4JUtils.deserialize;
import static org.fuin.units4j.Units4JUtils.serialize;
import static org.fuin.units4j.Units4JUtils.unmarshal;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link Data} class.
 */
// CHECKSTYLE:OFF Test
public class DataTest extends AbstractXmlTest {

    private static final String TYPE = "MyEvent";

    private static final VersionedMimeType MIME_TYPE = VersionedMimeType
            .create("application/xml; version=1; encoding=utf-8");

    private static final String CONTENT = "<myEvent/>";

    private Data testee;

    @Before
    public void setup() throws Exception {
        testee = new Data(TYPE, MIME_TYPE, CONTENT);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testSerializeDeserialize() {

        // PREPARE
        final Data original = testee;

        // TEST
        final byte[] data = serialize(original);
        final Data copy = deserialize(data);

        // VERIFY
        assertThat(copy.getType()).isEqualTo(TYPE);
        assertThat(copy.getMimeType()).isEqualTo(MIME_TYPE);
        assertThat(copy.getContent()).isEqualTo(CONTENT);

    }

    @Test
    public final void testMarshalUnmarshalXML() throws Exception {

        // PREPARE
        final Data original = testee;

        // TEST
        final String xml = marshalToStr(original, createXmlAdapter(),
                Data.class);

        // VERIFY
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert
                .assertXMLEqual(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                                + "<data type=\"MyEvent\" mime-type=\"application/xml; version=1; encoding=utf-8\">"
                                + "    <![CDATA[" + CONTENT + "]]>" + "</data>",
                        xml);

    }

    @Test
    public final void testMarshalUnmarshalEquals() throws Exception {

        // PREPARE
        final Data original = testee;

        // TEST
        final String xml = marshalToStr(original, createXmlAdapter(),
                Data.class);
        final Data copy = unmarshal(xml, createXmlAdapter(), Data.class);

        // VERIFY
        assertThat(copy.getType()).isEqualTo(TYPE);
        assertThat(copy.getMimeType()).isEqualTo(MIME_TYPE);
        assertThat(copy.getContent()).isEqualTo(CONTENT);

    }

    @Test
    public final void testIcon() throws Exception {

        // PREPARE
        final String type = "Icon";
        final VersionedMimeType mimeType = VersionedMimeType
                .create("image/png");
        final byte[] png = IOUtils.toByteArray(getClass().getResourceAsStream(
                "/ok.png"));
        final String content = Base64.encodeBase64String(png);
        final Data original = new Data(type, mimeType, content);

        // TEST
        final String xml = marshalToStr(original, createXmlAdapter(),
                Data.class);
        final Data copy = unmarshal(xml, createXmlAdapter(), Data.class);

        // VERIFY
        assertThat(copy.getType()).isEqualTo(type);
        assertThat(copy.getMimeType()).isEqualTo(mimeType);
        assertThat(copy.getContent()).isEqualTo(content);

    }

    private XmlAdapter<?, ?>[] createXmlAdapter() {
        return new XmlAdapter[] {};
    }

}
// CHECKSTYLE:ON
