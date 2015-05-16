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
package org.fuin.esc.intf;

import static org.fest.assertions.Assertions.assertThat;
import static org.fuin.units4j.Units4JUtils.deserialize;
import static org.fuin.units4j.Units4JUtils.marshal;
import static org.fuin.units4j.Units4JUtils.serialize;
import static org.fuin.units4j.Units4JUtils.unmarshal;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link Data} class.
 */
// CHECKSTYLE:OFF Test
public class DataTest {

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
        String xml = marshal(original, createXmlAdapter(), Data.class);
        xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<data type=\"MyEvent\" mime-type=\"application/xml; version=1; encoding=utf-8\">"
                + "<![CDATA[" + "<myEvent/>" + "]]>" + "</data>";

        final Data copy = unmarshal(xml, createXmlAdapter(), Data.class);

        // VERIFY
        assertThat(copy.getType()).isEqualTo(TYPE);
        assertThat(copy.getMimeType()).isEqualTo(MIME_TYPE);
        assertThat(copy.getContent()).isEqualTo(CONTENT);

    }

    private XmlAdapter<?, ?>[] createXmlAdapter() {
        return new XmlAdapter[] { new VersionedMimeTypeConverter() };
    }

    public static <T> String marshalToStr(final T data,
            final XmlAdapter<?, ?>[] adapters,
            final Class<?>... classesToBeBound) {
        try {
            final JAXBContext ctx = JAXBContext.newInstance(classesToBeBound);
            final Marshaller marshaller = ctx.createMarshaller();
            if (adapters != null) {
                for (XmlAdapter<?, ?> adapter : adapters) {
                    marshaller.setAdapter(adapter);
                }
            }
            final StringWriter writer = new StringWriter();
            final XMLOutputFactory xof = XMLOutputFactory.newInstance();
            final XMLStreamWriter sw = xof.createXMLStreamWriter(writer);
            final XMLStreamWriterAdapter swa = new XMLStreamWriterAdapter(sw) {
                @Override
                public void writeCharacters(String text)
                        throws XMLStreamException {
                    if (text.startsWith("<![CDATA[") && text.endsWith("]]>")) {
                        final String str = text.substring(9, text.length() - 3);
                        super.writeCData(str);
                    } else {
                        super.writeCharacters(text);
                    }
                }
            };
            marshaller.marshal(data, swa);
            return writer.toString();
        } catch (final JAXBException | XMLStreamException ex) {
            throw new RuntimeException("Error marshalling test data", ex);
        }
    }

}
// CHECKSTYLE:ON
