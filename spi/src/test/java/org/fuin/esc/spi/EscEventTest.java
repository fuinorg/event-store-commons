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

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

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
        final EscMetaData metaData = testee.getMeta();
        assertThat(metaData.getEscMeta().getUserMeta()).isNotNull();
        assertThat(metaData.getEscMeta().getUserMeta().getObj()).isInstanceOf(MyMeta.class);

        // TEST
        final String xml = marshal(testee, EscEvent.class, MyMeta.class, MyEvent.class, Base64Data.class);

        // VERIFY
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(expectedXml, xml);

    }

    @Test
    public final void testToJson() {

        // PREPARE
        final EnhancedMimeType dataContentType = EnhancedMimeType.create("application/xml");
        final EnhancedMimeType metaContentType = EnhancedMimeType
                .create("text/plain; transfer-encoding=base64");
        final String metaType = "JustText";
        final String base64Str = "SGVsbG8gd29ybGQh";
        final EscSysMeta sysMeta = new EscSysMeta(dataContentType, metaContentType, metaType);
        final DataWrapper userMeta = new DataWrapper(new Base64Data(base64Str));
        final EscMeta meta = new EscMeta(sysMeta, userMeta);
        final EscMetaData metaData = new EscMetaData(meta);
        final UUID eventId = UUID.randomUUID();
        final String eventType = "MyEvent";
        final String myEventId = "b2a936ce-d479-414f-b67f-3df4da383d47";
        final String myEventDescription = "Hello, JSON!";
        final JsonObject myEventFields = Json.createObjectBuilder().add("id", myEventId)
                .add("description", myEventDescription).build();
        final JsonObject myEvent = Json.createObjectBuilder().add("my-event", myEventFields).build();
        final DataWrapper data = new DataWrapper(myEvent);
        final EscEvent testee = new EscEvent(eventId, eventType, data, metaData);

        // TEST
        final JsonObject result = testee.toJson();

        // VERIFY
        assertThat(result.getString("EventId")).isEqualTo(eventId.toString());
        assertThat(result.getString("EventType")).isEqualTo(eventType);
        final JsonObject jsonData = result.getJsonObject("Data");
        assertThat(jsonData.getJsonObject("my-event")).isNotNull();
        final JsonObject jsonMyEvent = jsonData.getJsonObject("my-event");
        assertThat(jsonMyEvent.getString("id")).isEqualTo(myEventId);
        assertThat(jsonMyEvent.getString("description")).isEqualTo(myEventDescription);
        final JsonObject jsonMetaData = result.getJsonObject("MetaData");
        assertThat(jsonMetaData.getJsonObject("EscMeta")).isNotNull();
        final JsonObject jsonMeta = jsonMetaData.getJsonObject("EscMeta");
        assertThat(jsonMeta.getJsonObject("EscSysMeta")).isNotNull();
        final JsonObject jsonSysMeta = jsonMeta.getJsonObject("EscSysMeta");
        assertThat(jsonMeta.getJsonObject("EscUserMeta")).isNotNull();
        final JsonObject jsonUserMeta = jsonMeta.getJsonObject("EscUserMeta");
        assertThat(jsonSysMeta.getString("data-content-type")).isEqualTo(dataContentType.toString());
        assertThat(jsonSysMeta.getString("meta-content-type")).isEqualTo(metaContentType.toString());
        assertThat(jsonSysMeta.getString("meta-type")).isEqualTo(metaType);
        assertThat(jsonUserMeta.getString("Base64")).isEqualTo(base64Str);

    }

}
