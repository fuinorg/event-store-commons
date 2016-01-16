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

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.esc.eshttp.ESHttpUtils.createDocumentBuilder;
import static org.fuin.esc.eshttp.ESHttpUtils.createXPath;
import static org.fuin.esc.eshttp.ESHttpUtils.findNode;
import static org.fuin.esc.eshttp.ESHttpUtils.parseDocument;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeTypeParseException;
import javax.json.JsonObject;
import javax.xml.xpath.XPath;

import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Tests the {@link ESHttpXmlUnmarshaller} class.
 */
// CHECKSTYLE:OFF Test
public class ESHttpXmlUnmarshallerTest extends AbstractESHttpMarshallerTest {

    @Test
    public void testNull() throws MimeTypeParseException {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

        // TEST & VERIFY
        assertThat(
                new ESHttpXmlUnmarshaller().unmarshal(registry, new SerializedDataType("whatever"),
                        new EnhancedMimeType("application/xml"), null)).isNull();

    }

    @Test
    public void testXmlXml() throws IOException, MimeTypeParseException {

        // PREPARE
        final SerializedDataType dataType = new SerializedDataType(MyEvent.TYPE.asBaseType());
        final EnhancedMimeType mimeType = new EnhancedMimeType("application/xml");
        final DeserializerRegistry registry = createRegistry();
        final Node node = parse("/event-xml-xml-xml.xml", "/Event/Data/my-event");

        // TEST
        final Object obj = new ESHttpXmlUnmarshaller().unmarshal(registry, dataType, mimeType, node);

        // VERIFY
        assertThat(obj).isInstanceOf(MyEvent.class);
        final MyEvent event = (MyEvent) obj;
        assertThat(event.getId()).isEqualTo("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        assertThat(event.getDescription()).isEqualTo("Hello, XML!");

    }

    @Test
    public void testXmlOther() throws IOException, MimeTypeParseException {

        // PREPARE
        final SerializedDataType dataType = new SerializedDataType(MyEvent.TYPE.asBaseType());
        final EnhancedMimeType mimeType = new EnhancedMimeType("application/json; version=1; encoding=utf-8; transfer-encoding=base64");
        final DeserializerRegistry registry = createRegistry();
        final Node node = parse("/event-xml-xml-other.xml", "/Event/Data");

        // TEST
        final Object obj = new ESHttpXmlUnmarshaller().unmarshal(registry, dataType, mimeType, node);

        // VERIFY
        assertThat(obj).isInstanceOf(JsonObject.class);
        final JsonObject outer = (JsonObject) obj;
        final JsonObject event = outer.getJsonObject("my-event");
        assertThat(event.getString("id")).isEqualTo("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        assertThat(event.getString("description")).isEqualTo("Hello, JSON!");

    }
    
    private Node parse(final String resource, final String expression) throws IOException {
        final InputStream in = this.getClass().getResourceAsStream(resource);
        try {
            final Document doc = parseDocument(createDocumentBuilder(), in);
            final XPath xPath = createXPath();
            return findNode(doc, xPath, expression);
        } finally {
            in.close();
        }
    }

    private DeserializerRegistry createRegistry() throws MimeTypeParseException {
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        final XmlDeSerializer xmlDeserializer = new XmlDeSerializer(MyEvent.class, MyMeta.class);
        registry.addDeserializer(new SerializedDataType("MyEvent"), "application/xml", xmlDeserializer);
        registry.addDeserializer(new SerializedDataType("MyMeta"), "application/xml", xmlDeserializer);
        final JsonDeSerializer jsonDeserializer = new JsonDeSerializer();
        registry.addDeserializer(new SerializedDataType("MyEvent"), "application/json", jsonDeserializer);
        registry.addDeserializer(new SerializedDataType("MyMeta"), "application/json", jsonDeserializer);
        return registry;
    }

}
// CHECKSTYLE:ON
