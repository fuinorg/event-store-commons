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
package org.fuin.esc.mem;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EscApiUtils;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventType;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link InMemoryEventStoreSync} class.
 */
// CHECKSTYLE:OFF Test
public class InMemoryEventStoreSyncTest {

    private InMemoryEventStoreSync testee;

    @Before
    public void setup() {
        testee = new InMemoryEventStoreSync(Executors.newCachedThreadPool());
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
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("One"));
        final CommonEvent eventTwo = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Two"));

        // TEST
        testee.appendToStream(streamId, 0, eventOne, eventTwo);

        // VERIFY
        final StreamEventsSlice slice = testee.readEventsForward(StreamId.ALL,
                0, 2);
        assertThat(slice.getEvents()).contains(eventOne, eventTwo);
        assertThat(slice.getFromEventNumber()).isEqualTo(0);
        assertThat(slice.getNextEventNumber()).isEqualTo(2);

    }

    @Test
    public void testReadEventsBackward() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("One"));
        final CommonEvent eventTwo = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Two"));
        final CommonEvent eventThree = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Three"));
        final CommonEvent eventFour = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Four"));
        final CommonEvent eventFive = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Five"));
        final int nextVersion = testee.appendToStream(streamId, 0, eventOne,
                eventTwo, eventThree, eventFour, eventFive);
        final int version = nextVersion - 1;

        // TEST Slice 1
        final StreamEventsSlice slice1 = testee.readEventsBackward(
                StreamId.ALL, version, 2);

        // VERIFY Slice 1
        assertThat(slice1.getEvents()).containsExactly(eventFive, eventFour);
        assertThat(slice1.getFromEventNumber()).isEqualTo(version);
        assertThat(slice1.getNextEventNumber()).isEqualTo(version - 2);
        assertThat(slice1.isEndOfStream()).isFalse();

        // TEST Slice 2
        final StreamEventsSlice slice2 = testee.readEventsBackward(
                StreamId.ALL, slice1.getNextEventNumber(), 2);

        // VERIFY Slice 2
        assertThat(slice2.getEvents()).containsExactly(eventThree, eventTwo);
        assertThat(slice2.getFromEventNumber()).isEqualTo(version - 2);
        assertThat(slice2.getNextEventNumber()).isEqualTo(version - 4);
        assertThat(slice2.isEndOfStream()).isFalse();

        // TEST Slice 3
        final StreamEventsSlice slice3 = testee.readEventsBackward(
                StreamId.ALL, slice2.getNextEventNumber(), 2);

        // VERIFY Slice 3
        assertThat(slice3.getEvents()).containsExactly(eventOne);
        assertThat(slice3.getFromEventNumber()).isEqualTo(version - 4);
        assertThat(slice3.getNextEventNumber()).isEqualTo(version - 5);
        assertThat(slice3.isEndOfStream()).isTrue();

    }

    @Test
    public void testReadEventsForward() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("One"));
        final CommonEvent eventTwo = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Two"));
        final CommonEvent eventThree = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Three"));
        testee.appendToStream(streamId, 0, eventOne, eventTwo, eventThree);

        // TEST Slice 1
        final StreamEventsSlice slice1 = testee.readEventsForward(StreamId.ALL,
                0, 2);

        // VERIFY Slice 1
        assertThat(slice1.getEvents()).containsExactly(eventOne, eventTwo);
        assertThat(slice1.getFromEventNumber()).isEqualTo(0);
        assertThat(slice1.getNextEventNumber()).isEqualTo(2);
        assertThat(slice1.isEndOfStream()).isFalse();

        // TEST Slice 2
        final StreamEventsSlice slice2 = testee.readEventsForward(StreamId.ALL,
                slice1.getNextEventNumber(), 2);

        // VERIFY Slice 2
        assertThat(slice2.getEvents()).containsExactly(eventThree);
        assertThat(slice2.getFromEventNumber()).isEqualTo(2);
        assertThat(slice2.getNextEventNumber()).isEqualTo(3);
        assertThat(slice2.isEndOfStream()).isTrue();

    }

    @Test
    public void testSubscribeToStreamNewEvents() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("One"));
        final CommonEvent eventTwo = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Two"));
        final CommonEvent eventThree = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Three"));
        testee.appendToStream(streamId, eventOne);
        final List<CommonEvent> result = new CopyOnWriteArrayList<>();

        // TEST
        testee.subscribeToStream(streamId, EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS,
                (subscription, event) -> {
                    result.add(event);
                }, (subscription, exception) -> {
                    // Not used
            });
        testee.appendToStream(streamId, eventTwo, eventThree);
        waitForResult(result, 1);

        // VERIFY
        assertThat(result).containsExactly(eventTwo, eventThree);

    }

    @Test
    public void testSubscribeToStreamFromFirst() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Eins"));
        final CommonEvent eventTwo = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Zwei"));
        final CommonEvent eventThree = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Drei"));
        testee.appendToStream(streamId, eventOne, eventTwo, eventThree);
        final List<CommonEvent> result = new CopyOnWriteArrayList<>();

        // TEST
        testee.subscribeToStream(streamId, 0, (subscription, event) -> {
            result.add(event);
        }, (subscription, exception) -> {
            // Not used
            });
        waitForResult(result, 3);

        // VERIFY
        assertThat(result).containsExactly(eventOne, eventTwo, eventThree);

    }

    @Test
    public void testSubscribeToStreamFromX() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Eins"));
        final CommonEvent eventTwo = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Zwei"));
        final CommonEvent eventThree = new SimpleCommonEvent(new EventId(),
                new EventType("MyEvent"), new MyEvent("Drei"));
        testee.appendToStream(streamId, eventOne, eventTwo);
        final List<CommonEvent> result = new CopyOnWriteArrayList<>();

        // TEST
        testee.subscribeToStream(streamId, 1, (subscription, event) -> {
            result.add(event);
        }, (subscription, exception) -> {
            // Not used
            });
        testee.appendToStream(streamId, eventThree);
        waitForResult(result, 2);

        // VERIFY
        assertThat(result).containsExactly(eventTwo, eventThree);

    }

    @SuppressWarnings("unused")
    private void println(String prefix, List<CommonEvent> events) {
        System.out.println(prefix);
        for (CommonEvent event : events) {
            System.out.println(event + " {" + event.getData() + "}");
        }
    }

    private void waitForResult(final List<CommonEvent> result,
            final int expected) {
        int count = 0;
        while (result.size() != expected && (count < 10)) {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            count++;
        }

    }

}
// CHECKSTYLE:ON
