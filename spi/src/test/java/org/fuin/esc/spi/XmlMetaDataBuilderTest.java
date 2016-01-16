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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests the {@link XmlMetaDataBuilder} class.
 */
// CHECKSTYLE:OFF Test
public class XmlMetaDataBuilderTest {

    private XmlMetaDataBuilder testee;

    @Before
    public void setup() {
        testee = new XmlMetaDataBuilder();
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testAddInitNonNull() throws Exception {

        // PREPARE
        final String contentType = "application/xml; encoding=utf-8; version=1";
        final byte[] xml = "<meta><name>whatever</name><sub><foo>bar</foo></sub></meta>"
                .getBytes();
        final ByteArrayInputStream bais = new ByteArrayInputStream(xml);
        final DocumentBuilderFactory factory = DocumentBuilderFactory
                .newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(bais);

        // TEST
        testee.init(doc);
        testee.add("content-type", contentType);
        testee.add("encryption", false);
        testee.add("c", 789);

        // VERIFY
        final Document result = testee.build();
        assertThat(
                result.getElementsByTagName("content-type").item(0)
                        .getTextContent()).isEqualTo(contentType);
        assertThat(
                result.getElementsByTagName("encryption").item(0)
                        .getTextContent()).isEqualTo("false");
        assertThat(result.getElementsByTagName("c").item(0).getTextContent())
                .isEqualTo("789");
        assertThat(marshal(doc))
                .isEqualTo(
                        "<meta><name>whatever</name><sub><foo>bar</foo></sub>"
                                + "<content-type>application/xml; encoding=utf-8; version=1</content-type>"
                                + "<encryption>false</encryption><c>789</c></meta>");

    }

    @Test
    public void testAddInitWithNull() throws ParserConfigurationException {

        // PREPARE
        final String contentType = "application/xml; encoding=utf-8; version=1";

        // TEST
        testee.init(null);
        testee.add("content-type", contentType);
        testee.add("encryption", false);
        testee.add("c", 789);

        // VERIFY
        final Document result = testee.build();
        assertThat(
                result.getElementsByTagName("content-type").item(0)
                        .getTextContent()).isEqualTo(contentType);
        assertThat(
                result.getElementsByTagName("encryption").item(0)
                        .getTextContent()).isEqualTo("false");
        assertThat(result.getElementsByTagName("c").item(0).getTextContent())
                .isEqualTo("789");

    }

    private String marshal(final Document doc) throws TransformerException {
        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        final StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

}
