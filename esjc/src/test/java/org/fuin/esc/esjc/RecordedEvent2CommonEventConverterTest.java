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
package org.fuin.esc.esjc;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.RecordedEvent;
import com.github.msemys.esjc.proto.EventStoreClientMessages.EventRecord;
import com.github.msemys.esjc.util.UUIDConverter;
import com.google.protobuf.ByteString;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.spi.*;
import org.fuin.esc.spi.Base64Data.Base64DataJsonDeSerializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link RecordedEvent2CommonEventConverter} class.
 */
// CHECKSTYLE:OFF Test code
public class RecordedEvent2CommonEventConverterTest {

    private static final Class<?>[] JAXB_CLASSES = new Class<?>[] { EscMeta.class, MyEvent.class,
            MyMeta.class, Base64Data.class };

    /**
     * Tests envelope JSON + meta JSON + data JSON
     */
    @Test
    public final void testConvertJsonJsonJson() throws IOException {

        // PREPARE
        final EnhancedMimeType envelope = EnhancedMimeType.create("application", "json",
                Charset.forName("utf-8"));
        final int json = 1;
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(EscMeta.SER_TYPE, envelope.getBaseType(), new EscMetaJsonDeSerializer());
        registry.add(MyEvent.SER_TYPE, "application/json", new MyEvent.MyEventJsonDeSerializer());
        registry.add(MyMeta.SER_TYPE, "application/json", new MyMeta.MyMetaJsonDeSerializer());

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE,
                myEvent, MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry,
                envelope);
        final EventData eventData = converter.convert(commonEvent);

        final EventRecord eventRecord = EventRecord.newBuilder()
                .setEventId(ByteString.copyFrom(UUIDConverter.toBytes(eventData.eventId)))
                .setEventStreamId("mystream").setEventNumber(1).setEventType(eventData.type)
                .setDataContentType(json).setData(ByteString.copyFrom(eventData.data))
                .setMetadataContentType(1).setMetadata(ByteString.copyFrom(eventData.metadata))
                .setCreated(System.currentTimeMillis()).build();
        final RecordedEvent recordedEvent = new RecordedEvent(eventRecord);
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
     * Tests envelope JSON + meta JSON + data XML (non JSON)
     */
    @Test
    public final void testConvertJsonJsonOther() throws IOException {

        // PREPARE
        final EnhancedMimeType envelope = EnhancedMimeType.create("application", "json",
                Charset.forName("utf-8"));
        final int json = 1;
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(EscMeta.SER_TYPE, envelope.getBaseType(), new EscMetaJsonDeSerializer());
        registry.add(Base64Data.SER_TYPE, envelope.getBaseType(), new Base64DataJsonDeSerializer());
        registry.add(MyMeta.SER_TYPE, "application/json", new MyMeta.MyMetaJsonDeSerializer());
        registry.add(MyEvent.SER_TYPE, "application/xml", new XmlDeSerializer(JAXB_CLASSES));

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, XML!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE,
                myEvent, MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry,
                envelope);
        final EventData eventData = converter.convert(commonEvent);

        final EventRecord eventRecord = EventRecord.newBuilder()
                .setEventId(ByteString.copyFrom(UUIDConverter.toBytes(eventData.eventId)))
                .setEventStreamId("mystream").setEventNumber(1).setEventType(eventData.type)
                .setDataContentType(json).setData(ByteString.copyFrom(eventData.data))
                .setMetadataContentType(1).setMetadata(ByteString.copyFrom(eventData.metadata))
                .setCreated(System.currentTimeMillis()).build();
        final RecordedEvent recordedEvent = new RecordedEvent(eventRecord);
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
     * Tests envelope JSON + meta JSON + data XML (non JSON)
     */
    @Test
    public final void testConvertJsonOtherOther() throws IOException {

        // PREPARE
        final EnhancedMimeType envelope = EnhancedMimeType.create("application", "json",
                Charset.forName("utf-8"));
        final int json = 1;
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(EscMeta.SER_TYPE, envelope.getBaseType(), new EscMetaJsonDeSerializer());
        registry.add(Base64Data.SER_TYPE, envelope.getBaseType(), new Base64DataJsonDeSerializer());
        registry.add(MyMeta.SER_TYPE, "application/xml", new XmlDeSerializer(JAXB_CLASSES));
        registry.add(MyEvent.SER_TYPE, "application/xml", new XmlDeSerializer(JAXB_CLASSES));

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, XML!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE,
                myEvent, MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry,
                envelope);
        final EventData eventData = converter.convert(commonEvent);

        final EventRecord eventRecord = EventRecord.newBuilder()
                .setEventId(ByteString.copyFrom(UUIDConverter.toBytes(eventData.eventId)))
                .setEventStreamId("mystream").setEventNumber(1).setEventType(eventData.type)
                .setDataContentType(json).setData(ByteString.copyFrom(eventData.data))
                .setMetadataContentType(1).setMetadata(ByteString.copyFrom(eventData.metadata))
                .setCreated(System.currentTimeMillis()).build();
        final RecordedEvent recordedEvent = new RecordedEvent(eventRecord);
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
     * Tests envelope XML + meta XML + data XML
     */
    @Test
    public final void testConvertXmlXmlXml() throws IOException {

        // PREPARE
        final EnhancedMimeType envelope = EnhancedMimeType.create("application", "xml",
                Charset.forName("utf-8"));
        final int json = 0;
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(EscMeta.SER_TYPE, envelope.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
        registry.add(MyEvent.SER_TYPE, "application/xml", new XmlDeSerializer(JAXB_CLASSES));
        registry.add(MyMeta.SER_TYPE, "application/xml", new XmlDeSerializer(JAXB_CLASSES));

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE,
                myEvent, MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry,
                envelope);
        final EventData eventData = converter.convert(commonEvent);

        final EventRecord eventRecord = EventRecord.newBuilder()
                .setEventId(ByteString.copyFrom(UUIDConverter.toBytes(eventData.eventId)))
                .setEventStreamId("mystream").setEventNumber(1).setEventType(eventData.type)
                .setDataContentType(json).setData(ByteString.copyFrom(eventData.data))
                .setMetadataContentType(1).setMetadata(ByteString.copyFrom(eventData.metadata))
                .setCreated(System.currentTimeMillis()).build();
        final RecordedEvent recordedEvent = new RecordedEvent(eventRecord);
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
        final EnhancedMimeType envelope = EnhancedMimeType.create("application", "xml",
                Charset.forName("utf-8"));
        final int json = 0;
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(EscMeta.SER_TYPE, envelope.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
        registry.add(Base64Data.SER_TYPE, envelope.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
        registry.add(MyMeta.SER_TYPE, "application/xml", new XmlDeSerializer(JAXB_CLASSES));
        registry.add(MyEvent.SER_TYPE, "application/json", new MyEvent.MyEventJsonDeSerializer());

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, XML!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE,
                myEvent, MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry,
                envelope);
        final EventData eventData = converter.convert(commonEvent);

        final EventRecord eventRecord = EventRecord.newBuilder()
                .setEventId(ByteString.copyFrom(UUIDConverter.toBytes(eventData.eventId)))
                .setEventStreamId("mystream").setEventNumber(1).setEventType(eventData.type)
                .setDataContentType(json).setData(ByteString.copyFrom(eventData.data))
                .setMetadataContentType(1).setMetadata(ByteString.copyFrom(eventData.metadata))
                .setCreated(System.currentTimeMillis()).build();
        final RecordedEvent recordedEvent = new RecordedEvent(eventRecord);
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
     * Tests envelope XML + meta JSON + data JSON (non XML)
     */
    @Test
    public final void testConvertXmlOtherOther() throws IOException {

        // PREPARE
        final EnhancedMimeType envelope = EnhancedMimeType.create("application", "xml",
                Charset.forName("utf-8"));
        final int json = 0;
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(EscMeta.SER_TYPE, envelope.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
        registry.add(Base64Data.SER_TYPE, envelope.getBaseType(), new XmlDeSerializer(JAXB_CLASSES));
        registry.add(MyMeta.SER_TYPE, "application/json", new MyMeta.MyMetaJsonDeSerializer());
        registry.add(MyEvent.SER_TYPE, "application/json", new MyEvent.MyEventJsonDeSerializer());

        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE,
                myEvent, MyMeta.TYPE, myMeta);

        final CommonEvent2EventDataConverter converter = new CommonEvent2EventDataConverter(registry,
                envelope);
        final EventData eventData = converter.convert(commonEvent);

        final EventRecord eventRecord = EventRecord.newBuilder()
                .setEventId(ByteString.copyFrom(UUIDConverter.toBytes(eventData.eventId)))
                .setEventStreamId("mystream").setEventNumber(1).setEventType(eventData.type)
                .setDataContentType(json).setData(ByteString.copyFrom(eventData.data))
                .setMetadataContentType(1).setMetadata(ByteString.copyFrom(eventData.metadata))
                .setCreated(System.currentTimeMillis()).build();
        final RecordedEvent recordedEvent = new RecordedEvent(eventRecord);
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
// CHECKSTYLE:ON
