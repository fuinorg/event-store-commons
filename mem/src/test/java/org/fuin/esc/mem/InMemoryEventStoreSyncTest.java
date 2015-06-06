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

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Credentials;
import org.fuin.esc.api.EventId;
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
        testee = new InMemoryEventStoreSync();
        testee.open();
    }

    @After
    public void teardown() {
        testee.close();
        testee = null;
    }

    @Test
    public void testAppendToStreamArray() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = new CommonEvent(new EventId(), "MyEvent",
                new MyEvent("One"));
        final CommonEvent eventTwo = new CommonEvent(new EventId(), "MyEvent",
                new MyEvent("Two"));

        // TEST
        testee.appendToStream(Credentials.NONE, streamId, 0, eventOne, eventTwo);

        // VERIFY
        final StreamEventsSlice slice = testee.readEventsForward(
                Credentials.NONE, StreamId.ALL, 0, 2);
        assertThat(slice.getEvents()).contains(eventOne, eventTwo);
        assertThat(slice.getFromEventNumber()).isEqualTo(0);
        assertThat(slice.getNextEventNumber()).isEqualTo(2);

    }

    @Test
    public void testReadEventsBackward() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = new CommonEvent(new EventId(), "MyEvent",
                new MyEvent("One"));
        final CommonEvent eventTwo = new CommonEvent(new EventId(), "MyEvent",
                new MyEvent("Two"));
        final CommonEvent eventThree = new CommonEvent(new EventId(),
                "MyEvent", new MyEvent("Three"));
        final CommonEvent eventFour = new CommonEvent(new EventId(), "MyEvent",
                new MyEvent("Four"));
        final CommonEvent eventFive = new CommonEvent(new EventId(), "MyEvent",
                new MyEvent("Five"));
        final int version = testee.appendToStream(Credentials.NONE, streamId,
                0, eventOne, eventTwo, eventThree, eventFour, eventFive);

        // TEST Slice 1
        final StreamEventsSlice slice1 = testee.readEventsBackward(
                Credentials.NONE, StreamId.ALL, version, 2);

        // VERIFY Slice 1
        assertThat(slice1.getEvents()).containsExactly(eventFive, eventFour);
        assertThat(slice1.getFromEventNumber()).isEqualTo(version);
        assertThat(slice1.getNextEventNumber()).isEqualTo(version - 2);
        assertThat(slice1.isEndOfStream()).isFalse();

        // TEST Slice 2
        final StreamEventsSlice slice2 = testee.readEventsBackward(
                Credentials.NONE, StreamId.ALL, slice1.getNextEventNumber(), 2);

        // VERIFY Slice 2
        assertThat(slice2.getEvents()).containsExactly(eventThree, eventTwo);
        assertThat(slice2.getFromEventNumber()).isEqualTo(version - 2);
        assertThat(slice2.getNextEventNumber()).isEqualTo(version - 4);
        assertThat(slice2.isEndOfStream()).isFalse();

        // TEST Slice 3
        final StreamEventsSlice slice3 = testee.readEventsBackward(
                Credentials.NONE, StreamId.ALL, slice2.getNextEventNumber(), 2);

        // VERIFY Slice 3
        assertThat(slice3.getEvents()).containsExactly(eventOne);
        assertThat(slice3.getFromEventNumber()).isEqualTo(version - 4);
        assertThat(slice3.getNextEventNumber()).isEqualTo(version - 5);
        assertThat(slice3.isEndOfStream()).isTrue();

    }

    @Test
    public void testReadEventsForward() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = new CommonEvent(new EventId(), "MyEvent",
                new MyEvent("One"));
        final CommonEvent eventTwo = new CommonEvent(new EventId(), "MyEvent",
                new MyEvent("Two"));
        final CommonEvent eventThree = new CommonEvent(new EventId(),
                "MyEvent", new MyEvent("Three"));
        testee.appendToStream(Credentials.NONE, streamId, 0, eventOne,
                eventTwo, eventThree);

        // TEST Slice 1
        final StreamEventsSlice slice1 = testee.readEventsForward(
                Credentials.NONE, StreamId.ALL, 0, 2);

        // VERIFY Slice 1
        assertThat(slice1.getEvents()).containsExactly(eventOne, eventTwo);
        assertThat(slice1.getFromEventNumber()).isEqualTo(0);
        assertThat(slice1.getNextEventNumber()).isEqualTo(2);
        assertThat(slice1.isEndOfStream()).isFalse();

        // TEST Slice 2
        final StreamEventsSlice slice2 = testee.readEventsForward(
                Credentials.NONE, StreamId.ALL, slice1.getNextEventNumber(), 2);

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
            System.out.println(event + "{" + event.getData() + "}");
        }
    }

}
// CHECKSTYLE:ON
