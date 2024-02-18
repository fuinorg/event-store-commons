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

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.Position;
import com.eventstore.dbclient.RecordedEvent;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.jaxb.XmlDeSerializer;
import org.fuin.esc.jsonb.EscMeta;
import org.fuin.esc.jsonb.JsonbDeSerializer;
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

public class RecordedEvent2CommonEventConverterTest {

    private static final Class<?>[] JAXB_CLASSES = new Class<?>[]{
            org.fuin.esc.jaxb.EscMeta.class, MyEvent.class, MyMeta.class, org.fuin.esc.jaxb.Base64Data.class
    };

    private static final EnhancedMimeType JSON_UTF8 = EnhancedMimeType.create("application", "json", StandardCharsets.UTF_8);

    private static final EnhancedMimeType XML_UTF8 = EnhancedMimeType.create("application", "xml", StandardCharsets.UTF_8);

    /**
     * Tests envelope JSON + meta JSON + data JSON
     */
    @Test
    public final void testConvertJsonJsonJson() throws IOException {

        // PREPARE
        final SimpleSerializedDataTypeRegistry typeRegistry = createJsonTypeRegistry();
        try (final JsonbDeSerializer jsonbDeSer = TestUtils.createJsonbDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jsonbDeSer);

            final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
            registry.add(EscMeta.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);
            registry.add(MyEvent.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);
            registry.add(MyMeta.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);

            final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, JSON!");
            final MyMeta myMeta = new MyMeta("michael");
            final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                    MyMeta.TYPE, myMeta);

            final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry, new org.fuin.esc.jsonb.BaseTypeFactory(), JSON_UTF8);
            final EventData eventData = converter.convert(commonEvent);

            final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(), new Position(0, 0),
                    systemMetadata(eventData.getContentType(), 0, true, eventData.getEventType()), eventData.getEventData(),
                    eventData.getUserMetadata());
            final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(registry);

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

    }

    /**
     * Tests envelope JSON + meta JSON + data XML (non JSON)
     */
    @Test
    public final void testConvertJsonJsonOther() throws IOException {


        // PREPARE
        final SimpleSerializedDataTypeRegistry typeRegistry = createJsonTypeRegistry();
        try (final JsonbDeSerializer jsonbDeSer = TestUtils.createJsonbDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jsonbDeSer);

            final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
            registry.add(org.fuin.esc.jsonb.EscMeta.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);
            registry.add(org.fuin.esc.jsonb.Base64Data.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);
            registry.add(MyMeta.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);
            registry.add(MyEvent.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));

            final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, XML!");
            final MyMeta myMeta = new MyMeta("michael");
            final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                    MyMeta.TYPE, myMeta);

            final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry, new org.fuin.esc.jsonb.BaseTypeFactory(), JSON_UTF8);
            final EventData eventData = converter.convert(commonEvent);

            final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(),
                    new Position(0, 0),
                    systemMetadata(eventData.getContentType(), 0, false, eventData.getEventType()),
                    eventData.getEventData(), eventData.getUserMetadata());
            final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(registry);

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

    }

    /**
     * Tests envelope JSON + meta JSON + data XML (non JSON)
     */
    @Test
    public final void testConvertJsonOtherOther() throws IOException {

        // PREPARE
        final SimpleSerializedDataTypeRegistry typeRegistry = createJsonTypeRegistry();
        try (final JsonbDeSerializer jsonbDeSer = TestUtils.createJsonbDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jsonbDeSer);

            final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
            registry.add(org.fuin.esc.jsonb.EscMeta.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);
            registry.add(org.fuin.esc.jsonb.Base64Data.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);
            registry.add(MyMeta.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
            registry.add(MyEvent.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));

            final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, XML!");
            final MyMeta myMeta = new MyMeta("michael");
            final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                    MyMeta.TYPE, myMeta);

            final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry, new org.fuin.esc.jsonb.BaseTypeFactory(), JSON_UTF8);
            final EventData eventData = converter.convert(commonEvent);

            final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(), new Position(0, 0),
                    systemMetadata(eventData.getContentType(), 0, false, eventData.getEventType()),
                    eventData.getEventData(), eventData.getUserMetadata());
            final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(registry);

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

    }

    /**
     * Tests envelope XML + meta XML + data XML
     */
    @Test
    public final void testConvertXmlXmlXml() throws IOException {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(org.fuin.esc.jaxb.EscMeta.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
        registry.add(MyMeta.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
        registry.add(MyEvent.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry, new org.fuin.esc.jaxb.BaseTypeFactory(), XML_UTF8);
        final EventData eventData = converter.convert(commonEvent);

        final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(), new Position(0, 0),
                systemMetadata(eventData.getContentType(), 0, false, eventData.getEventType()),
                eventData.getEventData(), eventData.getUserMetadata());
        final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(registry);

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
        final SimpleSerializedDataTypeRegistry typeRegistry = createJaxbTypeRegistry();
        try (final JsonbDeSerializer jsonbDeSer = TestUtils.createJsonbDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jsonbDeSer);

            final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
            registry.add(org.fuin.esc.jaxb.EscMeta.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
            registry.add(org.fuin.esc.jaxb.Base64Data.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
            registry.add(MyMeta.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
            registry.add(MyEvent.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);

            final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, XML!");
            final MyMeta myMeta = new MyMeta("michael");
            final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                    MyMeta.TYPE, myMeta);

            final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry, new org.fuin.esc.jaxb.BaseTypeFactory(), XML_UTF8);
            final EventData eventData = converter.convert(commonEvent);

            final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(), new Position(0, 0),
                    systemMetadata(eventData.getContentType(), 0, true, eventData.getEventType()), eventData.getEventData(),
                    eventData.getUserMetadata());
            final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(registry);

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

    }

    /**
     * Tests envelope XML + meta JSON + data JSON (non XML)
     */
    @Test
    public final void testConvertXmlOtherOther() throws IOException {

        // PREPARE
        final SimpleSerializedDataTypeRegistry typeRegistry = createJaxbTypeRegistry();
        try (final JsonbDeSerializer jsonbDeSer = TestUtils.createJsonbDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jsonbDeSer);

            final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
            registry.add(org.fuin.esc.jaxb.EscMeta.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
            registry.add(org.fuin.esc.jaxb.Base64Data.SER_TYPE, XML_UTF8.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
            registry.add(MyMeta.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);
            registry.add(MyEvent.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);

            final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, JSON!");
            final MyMeta myMeta = new MyMeta("michael");
            final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                    MyMeta.TYPE, myMeta);

            final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry, new org.fuin.esc.jaxb.BaseTypeFactory(), XML_UTF8);
            final EventData eventData = converter.convert(commonEvent);

            final RecordedEvent recordedEvent = recordedEvent("mystream", 1, eventData.getEventId(), new Position(0, 0),
                    systemMetadata(eventData.getContentType(), 0, true, eventData.getEventType()), eventData.getEventData(),
                    eventData.getUserMetadata());
            final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(registry);

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

    }

    private static Map<String, String> systemMetadata(String contentType, long created, boolean json, String type) {
        final Map<String, String> map = new HashMap<>();
        map.put("content-type", contentType);
        map.put("created", "" + created);
        map.put("is-json", "" + json);
        map.put("type", type);
        return map;
    }

    private static RecordedEvent recordedEvent(String eventStreamId, long streamRevision, UUID eventId,
                                               Position position, Map<String, String> systemMetadata, byte[] eventData, byte[] userMetadata) {

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

    private static SimpleSerializedDataTypeRegistry createJsonTypeRegistry() {
        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(org.fuin.esc.jsonb.EscMeta.SER_TYPE, org.fuin.esc.jsonb.EscMeta.class);
        typeRegistry.add(org.fuin.esc.jsonb.Base64Data.SER_TYPE, org.fuin.esc.jsonb.Base64Data.class);
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);
        return typeRegistry;
    }

    private static SimpleSerializedDataTypeRegistry createJaxbTypeRegistry() {
        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(org.fuin.esc.jaxb.EscMeta.SER_TYPE, org.fuin.esc.jaxb.EscMeta.class);
        typeRegistry.add(org.fuin.esc.jaxb.Base64Data.SER_TYPE, org.fuin.esc.jaxb.Base64Data.class);
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);
        return typeRegistry;
    }

}

