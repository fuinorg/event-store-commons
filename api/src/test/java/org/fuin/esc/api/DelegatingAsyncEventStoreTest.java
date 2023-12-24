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
package org.fuin.esc.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test for {@link DelegatingAsyncEventStore}.
 */
// CHECKSTYLE:OFF Test
@RunWith(MockitoJUnitRunner.class)
public class DelegatingAsyncEventStoreTest {

    private static final long ANY_VERSION = ExpectedVersion.ANY.getNo();

    private DelegatingAsyncEventStore testee;

    @Mock
    private EventStore delegate;

    @Before
    public void setup() {
        testee = new DelegatingAsyncEventStore(Executors.newCachedThreadPool(), delegate);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testOpen() throws Exception {

        // TEST
        testee.open().get();

        // VERIFY
        verify(delegate).open();

    }

    @Test
    public void testClose() throws Exception {

        // TEST
        testee.close();

        // VERIFY
        verify(delegate).close();

    }

    @Test
    public void testAppendToStreamArrayExpectedVersion() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = event("One");
        final CommonEvent eventTwo = event("Two");
        when(delegate.appendToStream(streamId, ANY_VERSION, eventOne, eventTwo)).thenReturn(1L);

        // TEST
        final long result = testee.appendToStream(streamId, ANY_VERSION, eventOne, eventTwo).get();

        // VERIFY
        assertThat(result).isEqualTo(1L);
        verify(delegate).appendToStream(streamId, ANY_VERSION, eventOne, eventTwo);

    }

    @Test
    public void testAppendToStreamArrayAnyVersion() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = event("One");
        final CommonEvent eventTwo = event("Two");
        when(delegate.appendToStream(streamId, eventOne, eventTwo)).thenReturn(1L);

        // TEST
        final long result = testee.appendToStream(streamId, eventOne, eventTwo).get();

        // VERIFY
        assertThat(result).isEqualTo(1L);
        verify(delegate).appendToStream(streamId, eventOne, eventTwo);

    }
    
    @Test
    public void testReadEvent() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = event("One");
        when(delegate.readEvent(streamId, 0)).thenReturn(eventOne);

        // TEST
        final CommonEvent result = testee.readEvent(streamId, 0).get();

        // VERIFY
        assertThat(result).isEqualTo(eventOne);
        verify(delegate).readEvent(streamId, 0);

    }

    @Test
    public void testReadEventsForward() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final List<CommonEvent> events = new ArrayList<>();
        final StreamEventsSlice slice = new StreamEventsSlice(0, events, 1, true);
        when(delegate.readEventsForward(streamId, 0, 1)).thenReturn(slice);

        // TEST
        final StreamEventsSlice result = testee.readEventsForward(streamId, 0, 1).get();

        // VERIFY
        assertThat(result).isSameAs(slice);
        verify(delegate).readEventsForward(streamId, 0, 1);

    }

    @Test
    public void testReadEventsBackward() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final List<CommonEvent> events = new ArrayList<>();
        final StreamEventsSlice slice = new StreamEventsSlice(0, events, 1, true);
        when(delegate.readEventsBackward(streamId, 0, 1)).thenReturn(slice);

        // TEST
        final StreamEventsSlice result = testee.readEventsBackward(streamId, 0, 1).get();

        // VERIFY
        assertThat(result).isSameAs(slice);
        verify(delegate).readEventsBackward(streamId, 0, 1);

    }

    @Test
    public void testDeleteStreamExpectedVersion() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");

        // TEST
        testee.deleteStream(streamId, 0, true).get();

        // VERIFY
        verify(delegate).deleteStream(streamId, 0, true);

    }

    @Test
    public void testDeleteStreamAnyVersion() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");

        // TEST
        testee.deleteStream(streamId, true).get();

        // VERIFY
        verify(delegate).deleteStream(streamId, true);

    }

    @Test
    public void testAppendToStreamListExpectedVersion() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final List<CommonEvent> events = new ArrayList<>();
        when(delegate.appendToStream(streamId, ANY_VERSION, events)).thenReturn(1L);

        // TEST
        final long result = testee.appendToStream(streamId, ANY_VERSION, events).get();

        // VERIFY
        assertThat(result).isEqualTo(1L);
        verify(delegate).appendToStream(streamId, ANY_VERSION, events);

    }
    
    @Test
    public void testAppendToStreamListAnyVersion() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final List<CommonEvent> events = new ArrayList<>();
        when(delegate.appendToStream(streamId, events)).thenReturn(1L);

        // TEST
        final long result = testee.appendToStream(streamId, events).get();

        // VERIFY
        assertThat(result).isEqualTo(1L);
        verify(delegate).appendToStream(streamId, events);

    }

    @Test
    public void testStreamExists() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        when(delegate.streamExists(streamId)).thenReturn(true);

        // TEST
        final boolean result = testee.streamExists(streamId).get();

        // VERIFY
        assertThat(result).isTrue();
        verify(delegate).streamExists(streamId);

    }

    @Test
    public void testStreamState() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        when(delegate.streamState(streamId)).thenReturn(StreamState.ACTIVE);

        // TEST
        final StreamState result = testee.streamState(streamId).get();

        // VERIFY
        assertThat(result).isEqualTo(StreamState.ACTIVE);
        verify(delegate).streamState(streamId);

    }
    
    private static CommonEvent event(final String name) {
        return event(new EventId(), name);
    }

    private static CommonEvent event(final EventId id, final String name) {
        return new SimpleCommonEvent(id, new TypeName("MyEvent"), new MyEvent(name));
    }

}
// CHECKSTYLE:ON
