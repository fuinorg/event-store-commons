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
package org.fuin.esc.mem;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.*;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.core.KeyValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link InMemoryEventStore} class.
 */
public class InMemoryEventStoreTest {

    private InMemoryEventStore testee;

    @BeforeEach
    public void setup() {
        testee = new InMemoryEventStore(Executors.newCachedThreadPool());
        testee.open();
    }

    @AfterEach
    public void teardown() {
        testee.close();
        testee = null;
    }

    @Test
    public void testSameStreamId() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MySameStream");
        final StreamId otherId = new SampleStreamId("MySameStream");
        final CommonEvent eventOne = event("One");
        testee.createStream(streamId);

        // TEST
        testee.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), eventOne);

        // VERIFY
        assertThat(testee.streamExists(otherId)).isTrue();

    }

    @Test
    public void testAppendToStreamArray() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = event("One");
        final CommonEvent eventTwo = event("Two");

        // TEST
        testee.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), eventOne, eventTwo);

        // VERIFY
        final StreamEventsSlice slice = testee.readEventsForward(streamId, 0, 2);
        assertThat(slice.getEvents()).contains(eventOne, eventTwo);
        assertThat(slice.getFromEventNumber()).isEqualTo(0);
        assertThat(slice.getNextEventNumber()).isEqualTo(2);

    }

    @Test
    public void testReadEventsBackward() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = event("5874a43c-3404-42a1-a07e-64e22209963b", "One");
        final CommonEvent eventTwo = event("32383ba2-d524-4466-9ad2-f06a3fe1958f", "Two");
        final CommonEvent eventThree = event("8885c8c1-6148-4709-b816-018f72111559", "Three");
        final CommonEvent eventFour = event("4028e009-74eb-4a31-905a-3bdffb1046c0", "Four");
        final CommonEvent eventFive = event("9bfb6129-f503-49a7-b2d1-7a2edcefae9c", "Five");
        final long version = testee.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), eventOne, eventTwo, eventThree,
                eventFour, eventFive);

        // TEST Slice 1
        final StreamEventsSlice slice1 = testee.readEventsBackward(streamId, version, 2);

        // VERIFY Slice 1
        assertThat(slice1.getEvents()).containsExactly(eventFive, eventFour);
        assertThat(slice1.getFromEventNumber()).isEqualTo(version);
        assertThat(slice1.getNextEventNumber()).isEqualTo(version - 2);
        assertThat(slice1.isEndOfStream()).isFalse();

        // TEST Slice 2
        final StreamEventsSlice slice2 = testee.readEventsBackward(streamId, slice1.getNextEventNumber(), 2);

        // VERIFY Slice 2
        assertThat(slice2.getEvents()).containsExactly(eventThree, eventTwo);
        assertThat(slice2.getFromEventNumber()).isEqualTo(version - 2);
        assertThat(slice2.getNextEventNumber()).isEqualTo(version - 4);
        assertThat(slice2.isEndOfStream()).isFalse();

        // TEST Slice 3
        final StreamEventsSlice slice3 = testee.readEventsBackward(streamId, slice2.getNextEventNumber(), 2);

        // VERIFY Slice 3
        assertThat(slice3.getEvents()).containsExactly(eventOne);
        assertThat(slice3.getFromEventNumber()).isEqualTo(version - 4);
        assertThat(slice3.getNextEventNumber()).isEqualTo(version - 4);
        assertThat(slice3.isEndOfStream()).isTrue();

    }

    @Test
    public void testReadEventsForward() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = event("One");
        final CommonEvent eventTwo = event("Two");
        final CommonEvent eventThree = event("Three");
        testee.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), eventOne, eventTwo, eventThree);

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

    private static CommonEvent event(final String name) {
        return event(new EventId(), name);
    }

    private static CommonEvent event(final String uuid, final String name) {
        return event(new EventId(uuid), name);
    }

    private static CommonEvent event(final EventId id, final String name) {
        return new SimpleCommonEvent(id, new TypeName("MyEvent"), new MyEvent(name), null);
    }

    /**
     * Class to simulate different stream id type.
     */
    private static final class SampleStreamId implements StreamId {

        private static final long serialVersionUID = 1L;

        private final String name;

        public SampleStreamId(@NotNull final String name) {
            Contract.requireArgNotNull("name", name);
            this.name = name;
        }

        @Override
        public final String getName() {
            return name;
        }

        @Override
        public final boolean isProjection() {
            return false;
        }

        @Override
        public final <T> T getSingleParamValue() {
            throw new UnsupportedOperationException(getClass().getSimpleName() + " has no parameters");
        }

        @Override
        public final List<KeyValue> getParameters() {
            return Collections.emptyList();
        }

        @Override
        public final String asString() {
            return name;
        }

        @Override
        public final int hashCode() {
            return name.hashCode();
        }

        @Override
        public final boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SampleStreamId other = (SampleStreamId) obj;
            return name.equals(other.name);
        }

        @Override
        public final String toString() {
            return name;
        }

    }

}

