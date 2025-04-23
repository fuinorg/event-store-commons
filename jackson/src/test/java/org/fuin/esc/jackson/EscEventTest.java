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
package org.fuin.esc.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.activation.MimeTypeParseException;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.jaxb.XmlDeSerializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link EscEvent} class.
 */
public class EscEventTest {

    @Test
    public void testMarshalJackson() throws Exception {

        // PREPARE
        final String expectedJson = """
                {
                    "EventId":"b2a936ce-d479-414f-b67f-3df4da383d47",
                    "EventType":"MyEvent",
                    "Data":{
                        "id":"b2a936ce-d479-414f-b67f-3df4da383d47",
                        "description":"Hello, JSON!"
                    },
                    "MetaData":{
                        "data-type":"MyEvent",
                        "data-content-type":"application/json; version=1; encoding=UTF-8",
                        "meta-type":"MyMeta",
                        "meta-content-type":"application/json; version=1; encoding=UTF-8",
                        "MyMeta":{
                            "user":"abc"
                        }
                    }
                }
                """;

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final UUID eventId = UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47");
        final MyEvent myEvent = new MyEvent(UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47"), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("abc");

        final EnhancedMimeType dataContentType = EnhancedMimeType.create("application/json; version=1; encoding=UTF-8");
        final EnhancedMimeType metaContentType = EnhancedMimeType.create("application/json; version=1; encoding=UTF-8");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);
        final DataWrapper dataWrapper = new DataWrapper(myEvent);
        final DataWrapper metaWrapper = new DataWrapper(escMeta);
        final EscEvent event = new EscEvent(eventId, MyEvent.TYPE.asBaseType(), dataWrapper, metaWrapper);

        try (final JacksonDeSerializer jacksonDeSer = TestUtils.createJacksonDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jacksonDeSer);

            final ObjectMapper objectMapper = jacksonDeSer.getObjectMapper();

            // TEST
            final String currentJson = objectMapper.writeValueAsString(event);

            // VERIFY
            assertThatJson(currentJson).isEqualTo(expectedJson);

        }

    }

    @Test
    public final void testUnmarshalJackson() throws Exception {

        // PREPARE
        final String json = """
                {
                    "EventId":"b2a936ce-d479-414f-b67f-3df4da383d47",
                    "EventType":"MyEvent",
                    "Data":{
                        "id":"b2a936ce-d479-414f-b67f-3df4da383d47",
                        "description":"Hello, JSON!"
                    },
                    "MetaData":{
                        "data-type":"MyEvent",
                        "data-content-type":"application/json; version=1; encoding=UTF-8",
                        "meta-type":"MyMeta",
                        "meta-content-type":"application/json; version=1; encoding=UTF-8",
                        "MyMeta":{
                            "user":"abc"
                        }
                    }
                }
                """;

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        try (final JacksonDeSerializer jacksonDeSer = TestUtils.createJacksonDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jacksonDeSer);

            final ObjectMapper objectMapper = jacksonDeSer.getObjectMapper();

            // TEST
            final EscEvent testee = objectMapper.readValue(json, EscEvent.class);

            // VERIFY
            assertThat(testee.getEventId()).isEqualTo("b2a936ce-d479-414f-b67f-3df4da383d47");
            assertThat(testee.getEventType()).isEqualTo("MyEvent");
            assertThat(testee.getData().getObj()).isInstanceOf(MyEvent.class);
            final MyEvent myEvent = (MyEvent) testee.getData().getObj();
            assertThat(myEvent.getId()).isEqualTo("b2a936ce-d479-414f-b67f-3df4da383d47");
            assertThat(myEvent.getDescription()).isEqualTo("Hello, JSON!");
            assertThat(testee.getMeta().getObj()).isInstanceOf(EscMeta.class);
            final EscMeta escMeta = (EscMeta) testee.getMeta().getObj();
            assertThat(escMeta.getDataType()).isEqualTo("MyEvent");
            assertThat(escMeta.getDataContentType()).isEqualTo(EnhancedMimeType.create("application/json; version=1; encoding=UTF-8"));
            assertThat(escMeta.getMetaType()).isEqualTo("MyMeta");
            assertThat(escMeta.getMetaContentType()).isEqualTo(EnhancedMimeType.create("application/json; version=1; encoding=UTF-8"));
            assertThat(escMeta.getMeta()).isInstanceOf(MyMeta.class);
            final MyMeta myMeta = (MyMeta) escMeta.getMeta();
            assertThat(myMeta.getUser()).isEqualTo("abc");

        }
    }

    @Test
    public void testMarshalJacksonBase64() throws Exception {

        // PREPARE
        final String expectedJson = """
                {
                	"EventId": "68616d90-cf72-4c2a-b913-32bf6e6506ed",
                	"EventType": "MyEvent",
                	"Data": {
                		"Base64": "eyAibXktZXZlbnQiOiB7ICJpZCI6ICAiNjg2MTZkOTAtY2Y3Mi00YzJhLWI5MTMtMzJiZjZlNjUwNmVkIiwgImRlc2NyaXB0aW9uIjogIkhlbGxvLCBKU09OISIgfSB9"
                	},
                	"MetaData": {
                		"data-type": "MyEvent",
                		"data-content-type": "application/json; version=1; transfer-encoding=base64; encoding=UTF-8",
                		"meta-type": "MyMeta",
                		"meta-content-type": "application/json; version=1; encoding=UTF-8",
                		"MyMeta": {
                			"user": "abc"
                		}
                	}
                }
                """;

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final EscEvent event = createEventBase64();

        try (final JacksonDeSerializer jacksonDeSer = TestUtils.createJacksonDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jacksonDeSer);

            final ObjectMapper objectMapper = jacksonDeSer.getObjectMapper();

            // TEST
            final String currentJson = objectMapper.writeValueAsString(event);

            // VERIFY
            assertThatJson(currentJson).isEqualTo(expectedJson);

        }

    }

    @Test
    public final void testUnmarshalJacksonBase64() throws Exception {

        // PREPARE
        final String json = """
                {
                	"EventId": "68616d90-cf72-4c2a-b913-32bf6e6506ed",
                	"EventType": "MyEvent",
                	"Data": {
                		"Base64": "eyAibXktZXZlbnQiOiB7ICJpZCI6ICAiNjg2MTZkOTAtY2Y3Mi00YzJhLWI5MTMtMzJiZjZlNjUwNmVkIiwgImRlc2NyaXB0aW9uIjogIkhlbGxvLCBKU09OISIgfSB9"
                	},
                	"MetaData": {
                		"data-type": "MyEvent",
                		"data-content-type": "application/json; version=1; transfer-encoding=base64; encoding=UTF-8",
                		"meta-type": "MyMeta",
                		"meta-content-type": "application/json; version=1; encoding=UTF-8",
                		"MyMeta": {
                			"user": "abc"
                		}
                	}
                }
                """;

        final EscEvent expectedEvent = createEventBase64();

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        try (final JacksonDeSerializer jacksonDeSer = TestUtils.createJacksonDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jacksonDeSer);

            final ObjectMapper objectMapper = jacksonDeSer.getObjectMapper();

            // TEST
            final EscEvent testee = objectMapper.readValue(json, EscEvent.class);

            // VERIFY
            assertThat(testee.getEventId()).isEqualTo(expectedEvent.getEventId());
            assertThat(testee.getEventType()).isEqualTo(expectedEvent.getEventType());
            assertThat(testee.getData().getObj()).isInstanceOf(Base64Data.class);
            final Base64Data base64Data = (Base64Data) testee.getData().getObj();
            assertThat(base64Data.getEncoded()).isEqualTo(((Base64Data) expectedEvent.getData().getObj()).getEncoded());
            assertThat(testee.getMeta().getObj()).isInstanceOf(EscMeta.class);
            final EscMeta actualEscMeta = (EscMeta) testee.getMeta().getObj();
            final EscMeta expectedEscMeta = (EscMeta) expectedEvent.getMeta().getObj();
            assertThat(actualEscMeta.getDataType()).isEqualTo(expectedEscMeta.getDataType());
            assertThat(actualEscMeta.getDataContentType()).isEqualTo(expectedEscMeta.getDataContentType());
            assertThat(actualEscMeta.getMetaType()).isEqualTo(expectedEscMeta.getMetaType());
            assertThat(actualEscMeta.getMetaContentType()).isEqualTo(expectedEscMeta.getMetaContentType());
            assertThat(actualEscMeta.getMeta()).isInstanceOf(MyMeta.class);
            final MyMeta actualMyMeta = (MyMeta) actualEscMeta.getMeta();
            final MyMeta expectedMyMeta = (MyMeta) expectedEscMeta.getMeta();
            assertThat(actualMyMeta.getUser()).isEqualTo(expectedMyMeta.getUser());

        }

    }

    @Test
    public void testJsonBWithJaxB() throws IOException {

        final String expectedJson = """
                {
                  "EventId": "b2a936ce-d479-414f-b67f-3df4da383d47",
                  "EventType": "MyEvent",
                  "Data": "PG15RXZlbnQ+PGlkPmIyYTkzNmNlLWQ0NzktNDE0Zi1iNjdmLTNkZjRkYTM4M2Q0NzwvaWQ+PGRlc2NyaXB0aW9uPkhlbGxvLCBKU09OITwvZGVzY3JpcHRpb24+PC9teUV2ZW50Pg==",
                  "MetaData": {
                    "data-type": "MyEvent",
                    "data-content-type": "application/xml; encoding=UTF-8",
                    "meta-type": "MyMeta",
                    "meta-content-type": "application/xml; encoding=UTF-8",
                    "MyMeta": "PG15TWV0YT48aWQ+YWJjPC9pZD48L215TWV0YT4="
                  }
                }
                """;

        final UUID eventId = UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47");
        final MyEvent myEvent = new MyEvent(UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47"), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("abc");

        final EnhancedMimeType dataContentType = EnhancedMimeType.create("application/xml; encoding=UTF-8");
        final EnhancedMimeType metaContentType = EnhancedMimeType.create("application/xml; encoding=UTF-8");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType, myMeta);
        final DataWrapper dataWrapper = new DataWrapper(myEvent);
        final DataWrapper metaWrapper = new DataWrapper(escMeta);
        final EscEvent expectedEvent = new EscEvent(eventId, MyEvent.TYPE.asBaseType(), dataWrapper, metaWrapper);

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        try (final JacksonDeSerializer jacksonDeSer = TestUtils.createJacksonDeSerializer()) {
            // Create registry with known types
            final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
            // ESC types are serialized with JSON-B
            registry.add(EscEvents.SER_TYPE, jacksonDeSer.getMimeType().getBaseType(), jacksonDeSer);
            registry.add(EscEvent.SER_TYPE, jacksonDeSer.getMimeType().getBaseType(), jacksonDeSer);
            registry.add(EscMeta.SER_TYPE, jacksonDeSer.getMimeType().getBaseType(), jacksonDeSer);
            // User types are serialized with JAX-B
            final XmlDeSerializer jaxbDeSer = new XmlDeSerializer(MyEvent.class, MyMeta.class);
            registry.add(MyMeta.SER_TYPE, jaxbDeSer.getMimeType().getBaseType(), jaxbDeSer);
            registry.add(MyEvent.SER_TYPE, jaxbDeSer.getMimeType().getBaseType(), jaxbDeSer);
            jacksonDeSer.init(typeRegistry, registry, registry);
            final ObjectMapper objectMapper = jacksonDeSer.getObjectMapper();

            // Serialize
            final String json = objectMapper.writeValueAsString(expectedEvent);

            // Deserialize
            final EscEvent copy = objectMapper.readValue(json, EscEvent.class);

            // Verify
            assertThat(copy.getEventId()).isEqualTo(expectedEvent.getEventId());
            assertThat(copy.getEventType()).isEqualTo(expectedEvent.getEventType());
            assertThat(copy.getData().getObj()).isInstanceOf(MyEvent.class);
            final MyEvent copyMyEvent = (MyEvent) copy.getData().getObj();
            assertThat(copyMyEvent.getId()).isEqualTo(myEvent.getId());
            assertThat(copyMyEvent.getDescription()).isEqualTo(myEvent.getDescription());
            assertThat(copy.getMeta().getObj()).isInstanceOf(EscMeta.class);
            final EscMeta actualEscMeta = (EscMeta) copy.getMeta().getObj();
            final EscMeta expectedEscMeta = (EscMeta) expectedEvent.getMeta().getObj();
            assertThat(actualEscMeta.getDataType()).isEqualTo(expectedEscMeta.getDataType());
            assertThat(actualEscMeta.getDataContentType()).isEqualTo(expectedEscMeta.getDataContentType());
            assertThat(actualEscMeta.getMetaType()).isEqualTo(expectedEscMeta.getMetaType());
            assertThat(actualEscMeta.getMetaContentType()).isEqualTo(expectedEscMeta.getMetaContentType());
            assertThat(actualEscMeta.getMeta()).isInstanceOf(MyMeta.class);
            final MyMeta actualMyMeta = (MyMeta) actualEscMeta.getMeta();
            final MyMeta expectedMyMeta = (MyMeta) expectedEscMeta.getMeta();
            assertThat(actualMyMeta.getUser()).isEqualTo(expectedMyMeta.getUser());

            // Serialize the copy
            final String currentJson = objectMapper.writeValueAsString(copy);

            // Should be the same as before
            assertThatJson(currentJson).isEqualTo(expectedJson);

        }

    }

    private EscEvent createEventBase64() throws MimeTypeParseException {
        final UUID eventId = UUID.fromString("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        final Base64Data data = new Base64Data(
                "eyAibXktZXZlbnQiOiB7ICJpZCI6ICAiNjg2MTZkOTAtY2Y3Mi00YzJhLWI5MTMtMzJiZjZlNjUwNmVkIiwgImRlc2NyaXB0aW9uIjogIkhlbGxvLCBKU09OISIgfSB9");

        final MyMeta myMeta = new MyMeta("abc");

        final Map<String, String> params = new HashMap<>();
        params.put("transfer-encoding", "base64");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1", params);
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);
        return new EscEvent(eventId, MyEvent.TYPE.asBaseType(), new DataWrapper(data), new DataWrapper(escMeta));
    }

}

