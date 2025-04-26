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
package org.fuin.esc.esgrpc;

import io.kurrent.dbclient.EventData;
import io.kurrent.dbclient.Position;
import io.kurrent.dbclient.RecordedEvent;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.jaxb.EscJaxbUtils;
import org.fuin.esc.jaxb.XmlDeSerializer;
import org.fuin.esc.jsonb.EscJsonbUtils;
import org.fuin.esc.jsonb.JsonbSerDeserializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link RecordedEvent2CommonEventConverter} class.
 */

public class RecordedEvent2CommonEventConverterTest extends AbstractTest {

    private static final EnhancedMimeType JSON_UTF8 = EnhancedMimeType.create("application", "json", StandardCharsets.UTF_8);

    private static final EnhancedMimeType XML_UTF8 = EnhancedMimeType.create("application", "xml", StandardCharsets.UTF_8);

    /**
     * Tests envelope JSON + meta JSON + data JSON
     */
    @Test
    public final void testConvertJsonJsonJson() throws IOException {

        // PREPARE

        final SerializedDataTypeRegistry jsonTypeRegistry =
                EscJsonbUtils.addEscTypes(new SimpleSerializedDataTypeRegistry.Builder())
                .add(MyMeta.SER_TYPE, MyMeta.class)
                .add(MyEvent.SER_TYPE, MyEvent.class)
                .build();
        final JsonbSerDeserializer jsonbSerDeser = new JsonbSerDeserializer(getJsonbProvider(), jsonTypeRegistry, StandardCharsets.UTF_8);
        final SerDeserializerRegistry serDeserRegistry =
                // Envelope JSON
                EscJsonbUtils.addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(JSON_UTF8), jsonbSerDeser)
                // Meta JSON
                .add(MyMeta.SER_TYPE, jsonbSerDeser, jsonbSerDeser.getMimeType())
                // Data JSON
                .add(MyEvent.SER_TYPE, jsonbSerDeser, jsonbSerDeser.getMimeType())
                .build();
        TestUtils.register(getJsonbConfig(), serDeserRegistry, serDeserRegistry);

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(serDeserRegistry, new org.fuin.esc.jsonb.BaseTypeFactory(), JSON_UTF8);
        final EventData eventData = converter.convert(commonEvent);

        final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(), new Position(0, 0),
                systemMetadata(eventData.getContentType(), 0, true, eventData.getEventType()), eventData.getEventData(),
                eventData.getUserMetadata());
        final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(serDeserRegistry);

        // TEST
        final CommonEvent result = testee.convert(recordedEvent);

        // VERIFY
        assertThat(result.getId()).isEqualTo(new EventId(myEvent.getId()));
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE);
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE);

        assertThat(result.getData()).isInstanceOf(MyEvent.class);
        final MyEvent copyMyEvent = (MyEvent) result.getData();
        assertThat(copyMyEvent.getId()).isEqualTo(myEvent.getId());
        assertThat(copyMyEvent.getDescription()).isEqualTo(myEvent.getDescription());

        assertThat(result.getMeta()).isInstanceOf(MyMeta.class);
        final MyMeta copyMyMeta = (MyMeta) result.getMeta();
        assertThat(copyMyMeta.getUser()).isEqualTo(myMeta.getUser());

    }

    /**
     * Tests envelope JSON + meta JSON + data XML (non JSON)
     */
    @Test
    public final void testConvertJsonJsonOther() throws IOException {

        // PREPARE
        final SerializedDataTypeRegistry jsonTypeRegistry =
                // Envelope JSON
                EscJsonbUtils.addEscTypes(new SimpleSerializedDataTypeRegistry.Builder())
                        .add(MyMeta.SER_TYPE, MyMeta.class)
                        .build();
        final JsonbSerDeserializer jsonbSerDeser = new JsonbSerDeserializer(getJsonbProvider(), jsonTypeRegistry, StandardCharsets.UTF_8);
        final XmlDeSerializer xmlSerDeser = XmlDeSerializer.builder().add(MyEvent.class).build();
        final SerDeserializerRegistry serDeserRegistry =
                // Envelope JSON
                EscJsonbUtils.addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(JSON_UTF8), jsonbSerDeser)
                // Meta JSON
                .add(MyMeta.SER_TYPE, jsonbSerDeser, jsonbSerDeser.getMimeType())
                // Data XML
                .add(MyEvent.SER_TYPE, xmlSerDeser, xmlSerDeser.getMimeType())
                .build();
        TestUtils.register(getJsonbConfig(), serDeserRegistry, serDeserRegistry);

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, XML!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(serDeserRegistry, new org.fuin.esc.jsonb.BaseTypeFactory(), JSON_UTF8);
        final EventData eventData = converter.convert(commonEvent);

        final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(),
                new Position(0, 0),
                systemMetadata(eventData.getContentType(), 0, false, eventData.getEventType()),
                eventData.getEventData(), eventData.getUserMetadata());
        final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(serDeserRegistry);

        // TEST
        final CommonEvent result = testee.convert(recordedEvent);

        // VERIFY
        assertThat(result.getId()).isEqualTo(new EventId(myEvent.getId()));
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE);
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE);

        assertThat(result.getData()).isInstanceOf(MyEvent.class);
        final MyEvent copyMyEvent = (MyEvent) result.getData();
        assertThat(copyMyEvent.getId()).isEqualTo(myEvent.getId());
        assertThat(copyMyEvent.getDescription()).isEqualTo(myEvent.getDescription());

        assertThat(result.getMeta()).isInstanceOf(MyMeta.class);
        final MyMeta copyMyMeta = (MyMeta) result.getMeta();
        assertThat(copyMyMeta.getUser()).isEqualTo(myMeta.getUser());

    }

    /**
     * Tests envelope JSON + meta XML + data XML (non JSON)
     */
    @Test
    public final void testConvertJsonOtherOther() throws IOException {

        // PREPARE
        final SerializedDataTypeRegistry jsonTypeRegistry = EscJsonbUtils.addEscTypes(new SimpleSerializedDataTypeRegistry.Builder()).build();
        final JsonbSerDeserializer jsonbSerDeser = new JsonbSerDeserializer(getJsonbProvider(), jsonTypeRegistry, StandardCharsets.UTF_8);
        final XmlDeSerializer xmlSerDeser = XmlDeSerializer.builder().add(MyEvent.class).add(MyMeta.class).build();
        final SerDeserializerRegistry serDeserRegistry =
                // Envelope JSON
                EscJsonbUtils.addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(JSON_UTF8), jsonbSerDeser)
                // Meta XML
                .add(MyMeta.SER_TYPE, xmlSerDeser, xmlSerDeser.getMimeType())
                // Data XML
                .add(MyEvent.SER_TYPE, xmlSerDeser, xmlSerDeser.getMimeType())
                .build();
        TestUtils.register(getJsonbConfig(), serDeserRegistry, serDeserRegistry);

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, XML!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent, MyMeta.TYPE, myMeta);
        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(serDeserRegistry, new org.fuin.esc.jsonb.BaseTypeFactory(), JSON_UTF8);
        final EventData eventData = converter.convert(commonEvent);

        final RecordedEvent recordedEvent = recordedEvent(
                "mystream",
                1,
                eventData.getEventId(),
                new Position(0, 0),
                systemMetadata(eventData.getContentType(), 0,false, eventData.getEventType()),
                eventData.getEventData(),
                eventData.getUserMetadata());
        final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(serDeserRegistry);

        // TEST
        final CommonEvent result = testee.convert(recordedEvent);

        // VERIFY
        assertThat(result.getId()).isEqualTo(new EventId(myEvent.getId()));
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE);
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE);

        assertThat(result.getData()).isInstanceOf(MyEvent.class);
        final MyEvent copyMyEvent = (MyEvent) result.getData();
        assertThat(copyMyEvent.getId()).isEqualTo(myEvent.getId());
        assertThat(copyMyEvent.getDescription()).isEqualTo(myEvent.getDescription());

        assertThat(result.getMeta()).isInstanceOf(MyMeta.class);
        final MyMeta copyMyMeta = (MyMeta) result.getMeta();
        assertThat(copyMyMeta.getUser()).isEqualTo(myMeta.getUser());

    }

    /**
     * Tests envelope XML + meta XML + data XML
     */
    @Test
    public final void testConvertXmlXmlXml() throws IOException {

        // PREPARE
        final XmlDeSerializer xmlSerDeser = EscJaxbUtils.xmlDeSerializerBuilder().add(MyEvent.class).add(MyMeta.class).build();
        final SerDeserializerRegistry serDeserRegistry =
                // Envelope
                EscJaxbUtils.addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(XML_UTF8), xmlSerDeser)
                        // Meta XML
                        .add(MyMeta.SER_TYPE, xmlSerDeser, xmlSerDeser.getMimeType())
                        // Data XML
                        .add(MyEvent.SER_TYPE, xmlSerDeser, xmlSerDeser.getMimeType())
                        .build();

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(serDeserRegistry, new org.fuin.esc.jaxb.BaseTypeFactory(), XML_UTF8);
        final EventData eventData = converter.convert(commonEvent);

        final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(), new Position(0, 0),
                systemMetadata(eventData.getContentType(), 0, false, eventData.getEventType()),
                eventData.getEventData(), eventData.getUserMetadata());
        final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(serDeserRegistry);

        // TEST
        final CommonEvent result = testee.convert(recordedEvent);

        // VERIFY
        assertThat(result.getId()).isEqualTo(new EventId(myEvent.getId()));
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE);
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE);

        assertThat(result.getData()).isInstanceOf(MyEvent.class);
        final MyEvent copyMyEvent = (MyEvent) result.getData();
        assertThat(copyMyEvent.getId()).isEqualTo(myEvent.getId());
        assertThat(copyMyEvent.getDescription()).isEqualTo(myEvent.getDescription());

        assertThat(result.getMeta()).isInstanceOf(MyMeta.class);
        final MyMeta copyMyMeta = (MyMeta) result.getMeta();
        assertThat(copyMyMeta.getUser()).isEqualTo(myMeta.getUser());

    }

    /**
     * Tests envelope XML + meta XML + data JSON (non XML)
     */
    @Test
    public final void testConvertXmlXmlOther() throws IOException {

        // PREPARE
        final SerializedDataTypeRegistry jsonTypeRegistry =
                EscJsonbUtils.addEscTypes(new SimpleSerializedDataTypeRegistry.Builder())
                .add(MyEvent.SER_TYPE, MyEvent.class)
                .build();
        final JsonbSerDeserializer jsonbSerDeser = new JsonbSerDeserializer(getJsonbProvider(), jsonTypeRegistry, StandardCharsets.UTF_8);
        final XmlDeSerializer xmlSerDeser = EscJaxbUtils.xmlDeSerializerBuilder().add(MyMeta.class).build();
        final SerDeserializerRegistry serDeserRegistry =
                // Envelope XML
                EscJaxbUtils.addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(XML_UTF8), xmlSerDeser)
                        // Meta XML
                        .add(MyMeta.SER_TYPE, xmlSerDeser, xmlSerDeser.getMimeType())
                        // Data JSON
                        .add(MyEvent.SER_TYPE, jsonbSerDeser, jsonbSerDeser.getMimeType())
                        .build();
        TestUtils.register(getJsonbConfig(), serDeserRegistry, serDeserRegistry);

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, XML!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(serDeserRegistry, new org.fuin.esc.jaxb.BaseTypeFactory(), XML_UTF8);
        final EventData eventData = converter.convert(commonEvent);

        final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(), new Position(0, 0),
                systemMetadata(eventData.getContentType(), 0, true, eventData.getEventType()), eventData.getEventData(),
                eventData.getUserMetadata());
        final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(serDeserRegistry);

        // TEST
        final CommonEvent result = testee.convert(recordedEvent);

        // VERIFY
        assertThat(result.getId()).isEqualTo(new EventId(myEvent.getId()));
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE);
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE);

        assertThat(result.getData()).isInstanceOf(MyEvent.class);
        final MyEvent copyMyEvent = (MyEvent) result.getData();
        assertThat(copyMyEvent.getId()).isEqualTo(myEvent.getId());
        assertThat(copyMyEvent.getDescription()).isEqualTo(myEvent.getDescription());

        assertThat(result.getMeta()).isInstanceOf(MyMeta.class);
        final MyMeta copyMyMeta = (MyMeta) result.getMeta();
        assertThat(copyMyMeta.getUser()).isEqualTo(myMeta.getUser());

    }

    /**
     * Tests envelope XML + meta JSON + data JSON (non XML)
     */
    @Test
    public final void testConvertXmlOtherOther() throws IOException {

        // PREPARE
        final SerializedDataTypeRegistry jsonTypeRegistry = new SimpleSerializedDataTypeRegistry.Builder()
                .add(MyMeta.SER_TYPE, MyMeta.class)
                .add(MyEvent.SER_TYPE, MyEvent.class)
                .build();
        final JsonbSerDeserializer jsonbSerDeser = new JsonbSerDeserializer(getJsonbProvider(), jsonTypeRegistry, StandardCharsets.UTF_8);
        final XmlDeSerializer xmlSerDeser = EscJaxbUtils.xmlDeSerializerBuilder().build();
        final SerDeserializerRegistry serDeserRegistry =
                EscJaxbUtils.addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(XML_UTF8), xmlSerDeser)
                .add(MyMeta.SER_TYPE, jsonbSerDeser, jsonbSerDeser.getMimeType())
                .add(MyEvent.SER_TYPE, jsonbSerDeser, jsonbSerDeser.getMimeType())
                .build();
        TestUtils.register(getJsonbConfig(), serDeserRegistry, serDeserRegistry);

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(serDeserRegistry, new org.fuin.esc.jaxb.BaseTypeFactory(), XML_UTF8);
        final EventData eventData = converter.convert(commonEvent);

        final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(), new Position(0, 0),
                systemMetadata(eventData.getContentType(), 0, true, eventData.getEventType()), eventData.getEventData(),
                eventData.getUserMetadata());
        final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(serDeserRegistry);

        // TEST
        final CommonEvent result = testee.convert(recordedEvent);

        // VERIFY
        assertThat(result.getId()).isEqualTo(new EventId(myEvent.getId()));
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE);
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE);

        assertThat(result.getData()).isInstanceOf(MyEvent.class);
        final MyEvent copyMyEvent = (MyEvent) result.getData();
        assertThat(copyMyEvent.getId()).isEqualTo(myEvent.getId());
        assertThat(copyMyEvent.getDescription()).isEqualTo(myEvent.getDescription());

        assertThat(result.getMeta()).isInstanceOf(MyMeta.class);
        final MyMeta copyMyMeta = (MyMeta) result.getMeta();
        assertThat(copyMyMeta.getUser()).isEqualTo(myMeta.getUser());

    }

    private static Map<String, String> systemMetadata(String contentType, long created, boolean json, String type) {
        final Map<String, String> map = new HashMap<>();
        map.put("content-type", contentType);
        map.put("created", "" + created);
        map.put("is-json", "" + json);
        map.put("type", type);
        return map;
    }

    private static RecordedEvent recordedEvent(String eventStreamId,
                                               long streamRevision,
                                               UUID eventId,
                                               Position position,
                                               Map<String, String> systemMetadata,
                                               byte[] eventData,
                                               byte[] userMetadata) {

        try {
            final Constructor<RecordedEvent> constructor = RecordedEvent.class.getDeclaredConstructor(String.class,
                    long.class, UUID.class, Position.class, Map.class, byte[].class, byte[].class);
            constructor.setAccessible(true);
            return constructor.newInstance(eventStreamId, streamRevision, eventId, position, systemMetadata, eventData,
                    userMetadata);
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException
                 | IllegalArgumentException ex) {
            throw new RuntimeException("Failed to create " + RecordedEvent.class.getSimpleName(), ex);
        }
    }

}

