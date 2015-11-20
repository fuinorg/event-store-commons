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
package org.fuin.esc.eshttp;

import java.io.IOException;
import java.util.UUID;

import javax.activation.MimeTypeParseException;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.fuin.esc.api.EventId;
import org.fuin.esc.spi.SerializedData;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Tests the {@link ESHttpXmlMarshaller} class.
 */
// CHECKSTYLE:OFF Test
public class ESHttpXmlMarshallerTest extends AbstractESHttpMarshallerTest {

    @Test
    public void testXmlMetaXmlDataXml() throws IOException, MimeTypeParseException, SAXException {

        // PREPARE
        final String expectedXml = IOUtils.toString(this.getClass().getResourceAsStream(
                "/event-xml-xml-xml.xml"));

        final UUID uuid = UUID.fromString("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        final SerializedData serData = asXml(MyEvent.TYPE.asBaseType(),
                "application/xml; version=1; encoding=utf-8", createMyEventXml(uuid, "Hello, XML!"));
        final SerializedData serMeta = asXml(MyMeta.TYPE, "application/xml; version=1; encoding=utf-8",
                createMyMetaXml());

        // TEST
        final String xml = new ESHttpXmlMarshaller().marshalIntern(new EventId(uuid), MyEvent.TYPE, serData,
                serMeta);

        // VERIFY
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(expectedXml, xml);

    }

    @Test
    public void testXmlMetaXmlDataOther() throws IOException, MimeTypeParseException, SAXException {

        // PREPARE
        final String expectedXml = IOUtils.toString(this.getClass().getResourceAsStream(
                "/event-xml-xml-other.xml"));

        final UUID uuid = UUID.fromString("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        final SerializedData serData = asJson(MyEvent.TYPE.asBaseType(),
                "application/json; encoding=utf-8; version=1", createMyEventJson(uuid, "Hello, JSON!"));
        final SerializedData serMeta = asXml(MyMeta.TYPE, "application/xml; version=1; encoding=utf-8",
                createMyMetaXml());

        // TEST
        final String xml = new ESHttpXmlMarshaller().marshalIntern(new EventId(uuid), MyEvent.TYPE, serData,
                serMeta);

        // VERIFY
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(expectedXml, xml);

    }

    @Test
    public void testXmlMetaOtherDataOther() throws IOException, MimeTypeParseException, SAXException {

        // PREPARE
        final String expectedXml = IOUtils.toString(this.getClass().getResourceAsStream(
                "/event-xml-other-other.xml"));

        final UUID uuid = UUID.fromString("48ff75a0-d573-44eb-ad89-45b99757ddfc");
        final SerializedData serData = asJson(MyEvent.TYPE.asBaseType(),
                "application/json; encoding=utf-8; version=1", createMyEventJson(uuid, "Hello, JSON!"));
        final SerializedData serMeta = asJson(MyMeta.TYPE, "application/json; version=1; encoding=utf-8",
                createMyMetaJson());

        // TEST
        final String xml = new ESHttpXmlMarshaller().marshalIntern(new EventId(uuid), MyEvent.TYPE, serData,
                serMeta);

        // VERIFY
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(expectedXml, xml);

    }

}
// CHECKSTYLE:ON
