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

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link InMemoryEventStore} class.
 */
// CHECKSTYLE:OFF Test
public class InMemoryEventStoreTest {

    private InMemoryEventStore testee;

    @Before
    public void setup() {
        testee = new InMemoryEventStore();
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
        final CommonEvent eventOne = new CommonEvent(
                "ba7ba6d7-90be-4271-bee2-b104cb38f39a", "MyEvent", new MyEvent(
                        "One"));
        final CommonEvent eventTwo = new CommonEvent(
                "7cdcb63a-c3ca-4a85-be2c-f5675222542d", "MyEvent", new MyEvent(
                        "Two"));
        
        // TEST
        testee.appendToStream(streamId, 0, eventOne, eventTwo);

        // VERIFY
        final StreamEventsSlice slice = testee.readAllEventsForward(0, 2);
        assertThat(slice.getEvents()).contains(eventOne, eventTwo);
        assertThat(slice.getFromEventNumber()).isEqualTo(0);
        assertThat(slice.getNextEventNumber()).isEqualTo(2);
        
    }

}
// CHECKSTYLE:ON
