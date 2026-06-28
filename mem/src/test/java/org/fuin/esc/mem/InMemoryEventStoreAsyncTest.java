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

import org.fuin.esc.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link InMemoryEventStoreAsync} class directly via its {@link java.util.concurrent.CompletableFuture} API.
 */
public class InMemoryEventStoreAsyncTest {

    private InMemoryEventStoreAsync testee;

    @BeforeEach
    public void setup() {
        testee = new InMemoryEventStoreAsync(Executors.newCachedThreadPool());
        testee.open();
    }

    @AfterEach
    public void teardown() {
        testee.close();
        testee = null;
    }

    @Test
    public void testAppendAndReadForward() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = event("One");
        final CommonEvent eventTwo = event("Two");

        // TEST
        final long version = testee.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), eventOne, eventTwo).get();

        // VERIFY
        assertThat(version).isEqualTo(1);
        final StreamEventsSlice slice = testee.readEventsForward(streamId, 0, 2).get();
        assertThat(slice.getEvents()).containsExactly(eventOne, eventTwo);
        assertThat(slice.getNextEventNumber()).isEqualTo(2);

    }

    @Test
    public void testSubscribeToStreamFromFirst() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = event("Eins");
        final CommonEvent eventTwo = event("Zwei");
        final CommonEvent eventThree = event("Drei");
        testee.appendToStream(streamId, eventOne, eventTwo, eventThree).join();
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
    public void testSubscribeToStreamNewEvents() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = event("One");
        final CommonEvent eventTwo = event("Two");
        final CommonEvent eventThree = event("Three");
        testee.appendToStream(streamId, eventOne).join();
        final List<CommonEvent> result = new CopyOnWriteArrayList<>();

        // TEST
        testee.subscribeToStream(streamId, EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS, (subscription, event) -> {
            result.add(event);
        }, (subscription, exception) -> {
            // Not used
        });
        testee.appendToStream(streamId, eventTwo, eventThree).join();
        waitForResult(result, 1);

        // VERIFY
        assertThat(result).containsExactly(eventTwo, eventThree);

    }

    // TODO Fix test
    @Disabled("Unstable - Fails sometimes")
    @Test
    public void testSubscribeToStreamFromX() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final CommonEvent eventOne = event("Eins");
        final CommonEvent eventTwo = event("Zwei");
        final CommonEvent eventThree = event("Drei");
        testee.appendToStream(streamId, eventOne, eventTwo).join();
        final List<CommonEvent> result = new CopyOnWriteArrayList<>();

        // TEST
        testee.subscribeToStream(streamId, 1, (subscription, event) -> {
            result.add(event);
        }, (subscription, exception) -> {
            // Not used
        });
        testee.appendToStream(streamId, eventThree).join();
        waitForResult(result, 2);

        // VERIFY
        assertThat(result).containsExactly(eventTwo, eventThree);

    }

    @Test
    public void testStreamStateOfUnknownStreamFailsTheFuture() {

        // TEST & VERIFY
        assertThatThrownBy(() -> testee.streamState(new SimpleStreamId("DoesNotExist")).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(StreamNotFoundException.class);

    }

    private void waitForResult(final List<CommonEvent> result, final int expected) {
        int count = 0;
        while (result.size() != expected && (count < 10)) {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException ex) { // NOSONAR
                throw new RuntimeException(ex);
            }
            count++;
        }
    }

    private static CommonEvent event(final String name) {
        return new SimpleCommonEvent(new EventId(), new TypeName("MyEvent"), new MyEvent(name), null);
    }

}
