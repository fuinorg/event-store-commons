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

import jakarta.activation.MimeTypeParseException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.commons.io.IOUtils;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.utils4j.jaxb.JaxbUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.Utils4J.deserialize;
import static org.fuin.utils4j.Utils4J.serialize;
import static org.fuin.utils4j.jaxb.JaxbUtils.marshal;
import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;

/**
 * Tests the {@link Data} class.
 */
public class DataTest extends AbstractXmlTest {

    private static final String TYPE = "MyEvent";

    private static final EnhancedMimeType MIME_TYPE = EnhancedMimeType
            .create("application/xml; version=1; encoding=utf-8");

    private static final String CONTENT = "<book-added-event><name>Shining</name><author>Stephen King</author></book-added-event>";

    private Data testee;

    @BeforeEach
    public void setup() throws Exception {
        testee = new Data(TYPE, MIME_TYPE, CONTENT);
    }

    @AfterEach
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
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<data type=\"MyEvent\" mime-type=\"application/xml; version=1; encoding=utf-8\">"
                + "    <![CDATA[" + CONTENT + "]]>" + "</data>";
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(xml)
                .ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences())
                .describedAs(documentDiff.toString()).isFalse();

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
        final EnhancedMimeType mimeType = EnhancedMimeType.create("image/png");
        final byte[] png = IOUtils.toByteArray(getClass().getResourceAsStream("/ok.png"));
        final String content = Base64.getEncoder().encodeToString(png);
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

    @Test
    public void testUnmarshalXmlContent() throws JAXBException {

        // TEST
        final BookAddedEvent event = unmarshal(JAXBContext.newInstance(BookAddedEvent.class), testee.getContent(), null);

        // VERIFY
        assertThat(event).isNotNull();
        assertThat(event.getName()).isEqualTo("Shining");
        assertThat(event.getAuthor()).isEqualTo("Stephen King");

    }

    @Test
    public void testValueOfXml() throws MimeTypeParseException {

        // PREPARE
        final BookAddedEvent event = new BookAddedEvent("Shining", "Stephen King");

        // TEST

        final String dataXml = marshal(event, event.getClass());
        final Data data = new Data("BookAddedEvent", EnhancedMimeType.create("application/xml; encoding=utf-8"), dataXml);

        // VERIFY
        assertThat(data.getType()).isEqualTo("BookAddedEvent");
        assertThat(data.getMimeType()).isEqualTo(
                new EnhancedMimeType("application/xml; encoding=utf-8"));
        assertThat(data.getContent()).isEqualTo(JaxbUtils.XML_PREFIX
                + "<book-added-event><name>Shining</name><author>Stephen King</author></book-added-event>");
        assertThat(data.isXml()).isTrue();
        assertThat(data.isJson()).isFalse();

    }

    private XmlAdapter<?, ?>[] createXmlAdapter() {
        return new XmlAdapter[]{};
    }

}

