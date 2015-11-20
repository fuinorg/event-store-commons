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

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

import java.io.IOException;
import java.util.UUID;

import javax.activation.MimeTypeParseException;

import org.fuin.esc.api.EventId;
import org.fuin.esc.spi.SerializedData;
import org.junit.Test;

/**
 * Tests the {@link ESHttpJsonMarshaller} class.
 */
// CHECKSTYLE:OFF Test
public class ESHttpJsonMarshallerTest extends AbstractESHttpMarshallerTest {

    @Test
    public void testJsonMetaJsonDataJson() throws IOException, MimeTypeParseException {

        // PREPARE
        final String expectedJson = loadJsonResource("/event-json-json-json.json");

        final UUID uuid = UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47");
        final SerializedData serData = asJson(MyEvent.TYPE.asBaseType(),
                "application/json; encoding=utf-8; version=1", createMyEventJson(uuid, "Hello, JSON!"));
        final SerializedData serMeta = asJson(MyMeta.TYPE, "application/json; version=1; encoding=utf-8",
                createMyMetaJson());

        // TEST
        final String json = new ESHttpJsonMarshaller().marshalIntern(new EventId(uuid), MyEvent.TYPE, serData,
                serMeta);

        // VERIFY
        assertThatJson(json).isEqualTo(expectedJson);

    }

    @Test
    public void testJsonMetaJsonDataOther() throws IOException, MimeTypeParseException {

        final String expectedJson = loadJsonResource("/event-json-json-other.json");
        final UUID uuid = UUID.fromString("bd58da40-9249-4b42-a077-10455b483c80");

        final SerializedData serData = asXml(MyEvent.TYPE.asBaseType(),
                "application/xml; version=1; encoding=utf-8", createMyEventXml(uuid, "Hello, XML!"));
        final SerializedData serMeta = asJson(MyMeta.TYPE, "application/json; version=1; encoding=utf-8",
                createMyMetaJson());

        // TEST
        final String json = new ESHttpJsonMarshaller().marshalIntern(new EventId(uuid), MyEvent.TYPE, serData,
                serMeta);

        // VERIFY
        assertThatJson(json).isEqualTo(expectedJson);

    }

    @Test
    public void testJsonMetaOtherDataOther() throws IOException, MimeTypeParseException {

        final String expectedJson = loadJsonResource("/event-json-other-other.json");

        final UUID uuid = UUID.fromString("bf01c02f-5699-4e0f-8a9c-96343546d306");
        final SerializedData serData = asXml(MyEvent.TYPE.asBaseType(),
                "application/xml; version=1; encoding=utf-8", createMyEventJson(uuid, "Hello, JSON!"));
        final SerializedData serMeta = asXml(MyMeta.TYPE, "application/xml; version=1; encoding=utf-8",
                createMyMetaXml());

        // TEST
        final String json = new ESHttpJsonMarshaller().marshalIntern(new EventId(uuid), MyEvent.TYPE, serData,
                serMeta);

        // VERIFY
        assertThatJson(json).isEqualTo(expectedJson);

    }

}
// CHECKSTYLE:ON
