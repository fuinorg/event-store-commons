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
package org.fuin.esc.eshttp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import javax.activation.MimeTypeParseException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;
import org.junit.Test;

import com.jayway.jsonpath.JsonPath;

/**
 * Tests the {@link ESHttpJsonUnmarshaller} class.
 */
// CHECKSTYLE:OFF Test
public class ESHttpJsonUnmarshallerTest extends AbstractESHttpMarshallerTest {

    @Test
    public void testNull() throws MimeTypeParseException {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

        // TEST & VERIFY
        assertThat(
                new ESHttpJsonUnmarshaller().unmarshal(registry, new SerializedDataType("whatever"),
                        new EnhancedMimeType("application/json"), null)).isNull();

    }

    @Test
    public void testJsonJson() throws IOException, MimeTypeParseException {

        // PREPARE
        final SerializedDataType dataType = new SerializedDataType(MyEvent.TYPE.asBaseType());
        final EnhancedMimeType mimeType = new EnhancedMimeType("application/json");
        final DeserializerRegistry registry = createRegistry();
        final JsonObject jsonObj = parse("/event-json-json-json.json", "$.Data.my-event");

        // TEST
        final Object obj = new ESHttpJsonUnmarshaller().unmarshal(registry, dataType, mimeType, jsonObj);

        // VERIFY
        assertThat(obj).isInstanceOf(JsonObject.class);
        final JsonObject event = (JsonObject) obj;
        assertThat(event.getString("id")).isEqualTo("b2a936ce-d479-414f-b67f-3df4da383d47");
        assertThat(event.getString("description")).isEqualTo("Hello, JSON!");

    }

    @Test
    public void testJsonOther() throws IOException, MimeTypeParseException {

        // PREPARE
        final SerializedDataType dataType = new SerializedDataType(MyEvent.TYPE.asBaseType());
        final EnhancedMimeType mimeType = new EnhancedMimeType(
                "application/xml; version=1; encoding=utf-8; transfer-encoding=base64");
        final DeserializerRegistry registry = createRegistry();
        final JsonObject jsonObj = parse("/event-json-json-other.json", "$.Data");

        // TEST
        final Object obj = new ESHttpJsonUnmarshaller().unmarshal(registry, dataType, mimeType, jsonObj);

        // VERIFY
        assertThat(obj).isInstanceOf(MyEvent.class);
        final MyEvent event = (MyEvent) obj;
        assertThat(event.getId()).isEqualTo("bd58da40-9249-4b42-a077-10455b483c80");
        assertThat(event.getDescription()).isEqualTo("Hello, XML!");

    }

    private JsonObject parse(final String resource, final String expression) throws IOException {
        final InputStream in = this.getClass().getResourceAsStream(resource);
        final Reader reader = new InputStreamReader(in, Charset.forName("utf-8"));
        final JsonReader jsonReader = Json.createReader(reader);
        try {
            final JsonObject jsonObj = (JsonObject) jsonReader.read();
            return JsonPath.read(jsonObj, expression);
        } finally {
            jsonReader.close();
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
