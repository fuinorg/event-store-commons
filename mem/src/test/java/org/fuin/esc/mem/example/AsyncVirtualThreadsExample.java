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
package org.fuin.esc.mem.example;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EscApiUtils;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.Subscription;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.mem.InMemoryEventStoreAsync;
import org.fuin.utils4j.TestOmitted;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Self-contained example showing how to use the asynchronous in-memory event store with virtual threads.
 * No external server is required - just run the {@link #main(String[])} method (Java 21+).
 */
@TestOmitted("Example class")
@SuppressWarnings("java:S106") // System.out is fine for an example
public final class AsyncVirtualThreadsExample {

    private AsyncVirtualThreadsExample() {
    }

    /**
     * Runs the example.
     *
     * @param args Not used.
     * @throws ExecutionException   Should not happen.
     * @throws InterruptedException Should not happen.
     */
    public static void main(final String[] args) throws ExecutionException, InterruptedException {

        System.out.println("BEGIN");

        // 'callbackExecutor' is used by the in-memory store to DELIVER subscription callbacks: with a
        // virtual-thread-per-task executor every 'onEvent' runs on its own virtual thread.
        // 'workExecutor' is used below to fan out store operations, blocking cheaply on virtual threads.
        try (var callbackExecutor = Executors.newVirtualThreadPerTaskExecutor();
             var workExecutor = Executors.newVirtualThreadPerTaskExecutor()) {

            final InMemoryEventStoreAsync es = new InMemoryEventStoreAsync(callbackExecutor);
            es.open().get();

            // 1) Plain async append + read - joining the futures is cheap on a virtual thread
            final StreamId streamId = new SimpleStreamId("orders-1");
            final long version = es.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                    event("OrderPlaced", "order-1")).get();
            final StreamEventsSlice slice = es.readEventsForward(streamId, 0, 100).get();
            System.out.println("version=" + version + " events=" + slice.getEvents().size());

            // 2) Subscribe to NEW events - the callbacks are delivered on 'callbackExecutor' (virtual threads)
            final List<CommonEvent> received = new CopyOnWriteArrayList<>();
            final Subscription subscription = es.subscribeToStream(streamId, EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS,
                    (sub, ce) -> received.add(ce), // runs on a virtual thread - blocking work here is fine
                    (sub, ex) -> { /* dropped */ }).get();

            // 3) Fan out many independent appends, each blocking on its own virtual thread
            final List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < 1_000; i++) {
                final StreamId sid = new SimpleStreamId("stream-" + i);
                final CommonEvent ce = event("Created", "id-" + i);
                futures.add(workExecutor.submit(() -> es.appendToStream(sid, ce).get()));
            }
            for (final Future<Long> future : futures) {
                future.get();
            }
            System.out.println("appended events to " + futures.size() + " streams concurrently");

            // 4) Append to the subscribed stream so the (virtual-thread) subscription delivers it
            es.appendToStream(streamId, event("OrderShipped", "order-1")).get();
            waitFor(received, 1);
            System.out.println("subscription received " + received.size() + " new event(s)");

            es.unsubscribeFromStream(subscription).get();
            es.close();
        }

        System.out.println("END");
    }

    private static CommonEvent event(final String type, final String id) {
        // The in-memory store keeps the data object as-is (no serialization), so any object works.
        return new SimpleCommonEvent(new EventId(), new TypeName(type), id, null);
    }

    private static void waitFor(final List<CommonEvent> result, final int expected) throws InterruptedException {
        int count = 0;
        while (result.size() < expected && count < 50) {
            Thread.sleep(20);
            count++;
        }
    }

}
