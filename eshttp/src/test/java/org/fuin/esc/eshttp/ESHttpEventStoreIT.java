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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link ESHttpEventStore} class.
 */
// CHECKSTYLE:OFF Test
public class ESHttpEventStoreIT {

    private ESHttpEventStore testee;

    @Before
    public void setup() throws MalformedURLException {

        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        final URL url = new URL("http://127.0.0.1:2113/");
        final XmlDeSerializer xmlDeSer = new XmlDeSerializer(false, MyMeta.class, MyEvent.class);
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(new SerializedDataType(MyEvent.TYPE.asBaseType()), "application/xml", xmlDeSer);
        registry.add(new SerializedDataType(MyMeta.TYPE.asBaseType()), "application/xml", xmlDeSer);

        testee = new ESHttpEventStore(threadFactory, url, ESEnvelopeType.XML, registry, registry);
        testee.open();
    }

    @After
    public void teardown() {
        testee.close();
        testee = null;
    }

    @Test
    public void testAppendToStreamArray() {

        // PREPARE
        final MyMeta meta = new MyMeta("john.doe");
        final StreamId streamId = new SimpleStreamId("MyStreamA");
        final MyEvent one = new MyEvent("One");
        final TypeName dataType = new TypeName("MyEvent");
        final TypeName metaType = new TypeName("MyMeta");
        final CommonEvent eventOne = new SimpleCommonEvent(new EventId(one.getId()), dataType, one, metaType,
                meta);
        final MyEvent two = new MyEvent("Two");
        final CommonEvent eventTwo = new SimpleCommonEvent(new EventId(two.getId()), dataType, two, metaType,
                meta);

        // TEST
        testee.appendToStream(streamId, eventOne, eventTwo);

        // VERIFY
        final StreamEventsSlice slice = testee.readEventsForward(streamId, 0, 2);
        assertThat(slice.getEvents()).contains(eventOne, eventTwo);
        assertThat(slice.getFromEventNumber()).isEqualTo(0);
        assertThat(slice.getNextEventNumber()).isEqualTo(2);

    }

    @Test
    public void testReadEventsBackward() {

        // PREPARE
        final MyMeta meta = new MyMeta("john.doe");
        final StreamId streamId = new SimpleStreamId("MyStreamB");
        final TypeName dataType = new TypeName("MyEvent");
        final TypeName metaType = new TypeName("MyMeta");
        final MyEvent one = new MyEvent("One");
        final CommonEvent eventOne = new SimpleCommonEvent(new EventId(one.getId()), dataType, one, metaType, meta);
        final MyEvent two = new MyEvent("Two");
        final CommonEvent eventTwo = new SimpleCommonEvent(new EventId(two.getId()), dataType, two, metaType, meta);
        final MyEvent three = new MyEvent("Three");
        final CommonEvent eventThree = new SimpleCommonEvent(new EventId(three.getId()), dataType, three, metaType, meta);
        final MyEvent four = new MyEvent("Four");
        final CommonEvent eventFour = new SimpleCommonEvent(new EventId(four.getId()), dataType, four, metaType, meta);
        final MyEvent five = new MyEvent("Five");
        final CommonEvent eventFive = new SimpleCommonEvent(new EventId(five.getId()), dataType, five, metaType, meta);
        testee.appendToStream(streamId, eventOne, eventTwo, eventThree, eventFour, eventFive);

        // TEST Slice 1
        final StreamEventsSlice slice1 = testee.readEventsBackward(streamId, 4, 2);

        // VERIFY Slice 1
        assertThat(slice1.getEvents()).containsExactly(eventFive, eventFour);
        assertThat(slice1.getFromEventNumber()).isEqualTo(4);
        assertThat(slice1.getNextEventNumber()).isEqualTo(2);
        assertThat(slice1.isEndOfStream()).isFalse();

        // TEST Slice 2
        final StreamEventsSlice slice2 = testee.readEventsBackward(streamId, slice1.getNextEventNumber(), 2);

        // VERIFY Slice 2
        assertThat(slice2.getEvents()).containsExactly(eventThree, eventTwo);
        assertThat(slice2.getFromEventNumber()).isEqualTo(2);
        assertThat(slice2.getNextEventNumber()).isEqualTo(0);
        assertThat(slice2.isEndOfStream()).isFalse();

        // TEST Slice 3
        final StreamEventsSlice slice3 = testee.readEventsBackward(streamId, slice2.getNextEventNumber(), 2);

        // VERIFY Slice 3
        assertThat(slice3.getEvents()).containsExactly(eventOne);
        assertThat(slice3.getFromEventNumber()).isEqualTo(0);
        assertThat(slice3.getNextEventNumber()).isEqualTo(0);
        assertThat(slice3.isEndOfStream()).isTrue();

    }

    @Test
    public void testReadEventsForward() {

        // PREPARE
        final MyMeta meta = new MyMeta("john.doe");
        final StreamId streamId = new SimpleStreamId("MyStreamC");
        final MyEvent one = new MyEvent("One");
        final TypeName dataType = new TypeName("MyEvent");
        final TypeName metaType = new TypeName("MyMeta");
        final CommonEvent eventOne = new SimpleCommonEvent(new EventId(one.getId()), dataType, one, metaType,
                meta);
        final MyEvent two = new MyEvent("Two");
        final CommonEvent eventTwo = new SimpleCommonEvent(new EventId(two.getId()), dataType, two, metaType,
                meta);
        final MyEvent three = new MyEvent("Three");
        final CommonEvent eventThree = new SimpleCommonEvent(new EventId(three.getId()), dataType, three);
        testee.appendToStream(streamId, eventOne, eventTwo, eventThree);

        // TEST Slice 1
        final StreamEventsSlice slice1 = testee.readEventsForward(streamId, 0, 2);

        // VERIFY Slice 1
        assertThat(slice1.getEvents()).containsExactly(eventOne, eventTwo);
        assertThat(slice1.getFromEventNumber()).isEqualTo(0);
        assertThat(slice1.getNextEventNumber()).isEqualTo(2);
        assertThat(slice1.isEndOfStream()).isFalse();

        // TEST Slice 2
        final StreamEventsSlice slice2 = testee.readEventsForward(streamId, slice1.getNextEventNumber(), 2);

        // VERIFY Slice 2
        assertThat(slice2.getEvents()).containsExactly(eventThree);
        assertThat(slice2.getFromEventNumber()).isEqualTo(2);
        assertThat(slice2.getNextEventNumber()).isEqualTo(3);
        assertThat(slice2.isEndOfStream()).isTrue();

    }

    @SuppressWarnings("unused")
    private void println(String prefix, List<CommonEvent> events) {
        System.out.println(prefix);
        for (CommonEvent event : events) {
            System.out.println(event + " {" + event.getData() + "}");
        }
    }

}
// CHECKSTYLE:ON
