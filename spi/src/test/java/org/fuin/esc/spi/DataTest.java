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
import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;
import static org.fuin.utils4j.Utils4J.deserialize;
import static org.fuin.utils4j.Utils4J.serialize;

import javax.activation.MimeTypeParseException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.fuin.units4j.Units4JUtils;
import org.fuin.utils4j.jaxb.UnmarshallerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/**
 * Tests the {@link Data} class.
 */
// CHECKSTYLE:OFF Test
public class DataTest extends AbstractXmlTest {

    private static final String TYPE = "MyEvent";

    private static final EnhancedMimeType MIME_TYPE = EnhancedMimeType.create("application/xml; version=1; encoding=utf-8");

    private static final String CONTENT = "<book-added-event><name>Shining</name><author>Stephen King</author></book-added-event>";

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
        final String xml = marshalToStr(original, createXmlAdapter(), Data.class);

        // VERIFY
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<data type=\"MyEvent\" mime-type=\"application/xml; version=1; encoding=utf-8\">" + "    <![CDATA[" + CONTENT + "]]>"
                + "</data>";
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(xml).ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences()).describedAs(documentDiff.toString()).isFalse();

    }

    @Test
    public final void testMarshalUnmarshalEquals() throws Exception {

        // PREPARE
        final Data original = testee;

        // TEST
        final String xml = marshalToStr(original, createXmlAdapter(), Data.class);
        final Data copy = unmarshal(new UnmarshallerBuilder().addClassesToBeBound(Data.class).addAdapters(createXmlAdapter())
                .withHandler(event -> false).build(), xml);

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
        final String content = Base64.encodeBase64String(png);
        final Data original = new Data(type, mimeType, content);

        // TEST
        final String xml = marshalToStr(original, createXmlAdapter(), Data.class);
        final Data copy = unmarshal(xml, createXmlAdapter(), Data.class);

        // VERIFY
        assertThat(copy.getType()).isEqualTo(type);
        assertThat(copy.getMimeType()).isEqualTo(mimeType);
        assertThat(copy.getContent()).isEqualTo(content);

    }

    @Test
    public void testUnmarshalXmlContent() throws JAXBException {

        // TEST
        final BookAddedEvent event = testee.unmarshalContent(JAXBContext.newInstance(BookAddedEvent.class));

        // VERIFY
        assertThat(event).isNotNull();
        assertThat(event.getName()).isEqualTo("Shining");
        assertThat(event.getAuthor()).isEqualTo("Stephen King");

    }

    @Test
    public void testUnmarshalJsonContent() throws MimeTypeParseException {

        // PREPARE
        final Data data = new Data("BookAddedEvent", new EnhancedMimeType("application/json; encoding=utf-8"),
                "{\"name\":\"Shining\",\"author\":\"Stephen King\"}");

        // TEST
        final JsonObject event = data.unmarshalContent(null);

        // VERIFY
        assertThat(event.getString("name")).isEqualTo("Shining");
        assertThat(event.getString("author")).isEqualTo("Stephen King");

    }

    @Test
    public void testValueOfXml() throws MimeTypeParseException {

        // PREPARE
        final BookAddedEvent event = new BookAddedEvent("Shining", "Stephen King");

        // TEST
        final Data data = Data.valueOf("BookAddedEvent", event);

        // VERIFY
        assertThat(data.getType()).isEqualTo("BookAddedEvent");
        assertThat(data.getMimeType()).isEqualTo(new EnhancedMimeType("application/xml; encoding=utf-8"));
        assertThat(data.getContent()).isEqualTo(
                Units4JUtils.XML_PREFIX + "<book-added-event><name>Shining</name><author>Stephen King</author></book-added-event>");
        assertThat(data.isXml()).isTrue();
        assertThat(data.isJson()).isFalse();

    }

    @Test
    public void testValueOfJson() throws MimeTypeParseException {

        // PREPARE
        final JsonObject event = Json.createObjectBuilder().add("name", "Shining").add("author", "Stephen King").build();

        // TEST
        final Data data = Data.valueOf("BookAddedEvent", event);

        // VERIFY
        assertThat(data.getType()).isEqualTo("BookAddedEvent");
        assertThat(data.getMimeType()).isEqualTo(new EnhancedMimeType("application/json; encoding=utf-8"));
        assertThat(data.getContent()).isEqualTo("{\"name\":\"Shining\",\"author\":\"Stephen King\"}");
        assertThat(data.isXml()).isFalse();
        assertThat(data.isJson()).isTrue();

    }

    private XmlAdapter<?, ?>[] createXmlAdapter() {
        return new XmlAdapter[] {};
    }

}
// CHECKSTYLE:ON
