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
package org.fuin.esc.spi;

import org.fuin.esc.api.Backoff;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EscApiUtils;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.SubscribableEventStoreAsync;
import org.fuin.esc.api.Subscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link ReconnectingSubscribableEventStore}.
 */
public final class ReconnectingSubscribableEventStoreTest {

    private static final StreamId STREAM = new SimpleStreamId("MyStream");

    /** Fast schedule without jitter so the assertions stay deterministic. */
    private static final Backoff FAST = new Backoff(Duration.ofMillis(5), Duration.ofMillis(20), 2.0, 0.0,
            Backoff.UNLIMITED_ATTEMPTS);

    private ScheduledExecutorService scheduler;

    private FakeStore fake;

    @BeforeEach
    public void setup() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        fake = new FakeStore();
    }

    @AfterEach
    public void teardown() {
        scheduler.shutdownNow();
    }

    @Test
    public void testReconnectResumesAfterTheLastDeliveredEvent() throws Exception {

        // PREPARE
        final ReconnectingSubscribableEventStore testee =
                new ReconnectingSubscribableEventStore(fake, scheduler, FAST);
        final List<CommonEvent> received = Collections.synchronizedList(new ArrayList<>());
        final Subscription handle = testee.subscribeToStream(STREAM, 10,
                (sub, event) -> received.add(event), (sub, ex) -> { }).get(5, TimeUnit.SECONDS);
        fake.deliver(event("one"));
        fake.deliver(event("two"));

        // TEST
        fake.dropCurrent(new IllegalStateException("connection lost"));

        // VERIFY: two events were consumed from 10, so the new subscription starts at 12
        fake.awaitSubscribeCount(2);
        assertThat(fake.startPositions()).containsExactly(10L, 12L);
        // The consumer keeps the handle it was given - the inner subscription changed underneath it.
        fake.deliver(event("three"));
        assertThat(received).hasSize(3);
        assertThat(handle.getStreamId()).isEqualTo(STREAM);
    }

    @Test
    public void testSubscribeToNewEventsStaysOnNewEvents() throws Exception {

        // PREPARE: a wake-up subscription has no absolute anchor to resume from
        final ReconnectingSubscribableEventStore testee =
                new ReconnectingSubscribableEventStore(fake, scheduler, FAST);
        testee.subscribeToStream(STREAM, EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS, (sub, event) -> { },
                (sub, ex) -> { }).get(5, TimeUnit.SECONDS);
        fake.deliver(event("one"));

        // TEST
        fake.dropCurrent(new IllegalStateException("connection lost"));

        // VERIFY
        fake.awaitSubscribeCount(2);
        assertThat(fake.startPositions()).containsExactly(
                (long) EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS, (long) EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS);
    }

    @Test
    public void testAnEventWhoseHandlerThrowsIsRedelivered() throws Exception {

        // PREPARE
        final ReconnectingSubscribableEventStore testee =
                new ReconnectingSubscribableEventStore(fake, scheduler, FAST);
        testee.subscribeToStream(STREAM, 0, (sub, event) -> {
            throw new IllegalStateException("handler failed");
        }, (sub, ex) -> { }).get(5, TimeUnit.SECONDS);
        assertThatThrownBy(() -> fake.deliver(event("one"))).isInstanceOf(IllegalStateException.class);

        // TEST
        fake.dropCurrent(new IllegalStateException("connection lost"));

        // VERIFY: the position was not advanced, so the failed event comes again
        fake.awaitSubscribeCount(2);
        assertThat(fake.startPositions()).containsExactly(0L, 0L);
    }

    @Test
    public void testRetriesUntilTheStoreAcceptsTheSubscriptionAgain() throws Exception {

        // PREPARE
        final ReconnectingSubscribableEventStore testee =
                new ReconnectingSubscribableEventStore(fake, scheduler, FAST);
        testee.subscribeToStream(STREAM, 0, (sub, event) -> { }, (sub, ex) -> { }).get(5, TimeUnit.SECONDS);

        // TEST: the store is unreachable for the first three reconnect attempts
        fake.failNextSubscribes(3);
        fake.dropCurrent(new IllegalStateException("connection lost"));

        // VERIFY: attempt 4 succeeds - 1 initial + 3 failed + 1 successful
        fake.awaitSubscribeCount(5);
        assertThat(fake.currentSubscription()).isNotNull();
    }

    @Test
    public void testGivesUpAfterMaxAttemptsAndTellsTheConsumer() throws Exception {

        // PREPARE
        final ReconnectingSubscribableEventStore testee = new ReconnectingSubscribableEventStore(
                fake, scheduler, FAST.withMaxAttempts(2));
        final CountDownLatch dropped = new CountDownLatch(1);
        final List<Exception> reported = Collections.synchronizedList(new ArrayList<>());
        testee.subscribeToStream(STREAM, 0, (sub, event) -> { }, (sub, ex) -> {
            reported.add(ex);
            dropped.countDown();
        }).get(5, TimeUnit.SECONDS);

        // TEST: the store never comes back
        fake.failNextSubscribes(100);
        fake.dropCurrent(new IllegalStateException("connection lost"));

        // VERIFY: the consumer is told once, after the attempts are used up
        assertThat(dropped.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(reported).hasSize(1);
        // 1 initial + 2 allowed attempts, then no further subscribe
        Thread.sleep(100);
        assertThat(fake.subscribeCount()).isEqualTo(3);
    }

    @Test
    public void testUnsubscribeStopsReconnecting() throws Exception {

        // PREPARE
        final ReconnectingSubscribableEventStore testee =
                new ReconnectingSubscribableEventStore(fake, scheduler, FAST);
        final Subscription handle = testee.subscribeToStream(STREAM, 0, (sub, event) -> { },
                (sub, ex) -> { }).get(5, TimeUnit.SECONDS);

        // TEST
        testee.unsubscribeFromStream(handle).get(5, TimeUnit.SECONDS);
        fake.dropCurrent(new IllegalStateException("connection lost"));

        // VERIFY: no reconnect for a subscription the consumer gave up itself
        Thread.sleep(100);
        assertThat(fake.subscribeCount()).isEqualTo(1);
    }

    @Test
    public void testInitialSubscribeIsNotRetried() {

        // PREPARE
        final ReconnectingSubscribableEventStore testee =
                new ReconnectingSubscribableEventStore(fake, scheduler, FAST);
        fake.failNextSubscribes(1);

        // TEST & VERIFY: the caller learns why its wiring did not come up
        assertThatThrownBy(() -> testee.subscribeToStream(STREAM, 0, (sub, event) -> { }, (sub, ex) -> { })
                .get(5, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(fake.subscribeCount()).isEqualTo(1);
    }

    @Test
    public void testUnsubscribePassesAForeignSubscriptionOn() throws Exception {

        // PREPARE
        final ReconnectingSubscribableEventStore testee =
                new ReconnectingSubscribableEventStore(fake, scheduler, FAST);
        testee.subscribeToStream(STREAM, 0, (sub, event) -> { }, (sub, ex) -> { }).get(5, TimeUnit.SECONDS);
        final Subscription inner = fake.currentSubscription();

        // TEST
        testee.unsubscribeFromStream(inner).get(5, TimeUnit.SECONDS);

        // VERIFY
        assertThat(fake.unsubscribed()).containsExactly(inner);
    }

    private static CommonEvent event(final String id) {
        return new SimpleCommonEvent(new EventId(), new TypeName("MyEvent"), id, null);
    }

    /**
     * Subscribable store whose subscriptions can be dropped and whose subscribe calls can be made to fail,
     * which is what a running store cannot be talked into doing on demand.
     */
    private static final class FakeStore implements SubscribableEventStoreAsync {

        private final List<Long> startPositions = Collections.synchronizedList(new ArrayList<>());

        private final List<Subscription> unsubscribed = Collections.synchronizedList(new ArrayList<>());

        private final AtomicInteger failures = new AtomicInteger();

        private volatile FakeSubscription current;

        private volatile BiConsumer<Subscription, CommonEvent> onEvent;

        private volatile BiConsumer<Subscription, Exception> onDrop;

        @Override
        public CompletableFuture<Subscription> subscribeToStream(final StreamId streamId, final long eventNumber,
                                                                 final BiConsumer<Subscription, CommonEvent> onEvent,
                                                                 final BiConsumer<Subscription, Exception> onDrop) {
            startPositions.add(eventNumber);
            if (failures.getAndUpdate(remaining -> remaining > 0 ? remaining - 1 : 0) > 0) {
                current = null;
                return CompletableFuture.failedFuture(new IllegalStateException("Store is not reachable"));
            }
            final FakeSubscription subscription = new FakeSubscription(streamId);
            this.current = subscription;
            this.onEvent = onEvent;
            this.onDrop = onDrop;
            return CompletableFuture.completedFuture(subscription);
        }

        @Override
        public CompletableFuture<Void> unsubscribeFromStream(final Subscription subscription) {
            unsubscribed.add(subscription);
            if (subscription == current) {
                current = null;
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> open() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void close() {
            // Nothing to do
        }

        void deliver(final CommonEvent event) {
            onEvent.accept(current, event);
        }

        void dropCurrent(final Exception cause) {
            current = null;
            onDrop.accept(null, cause);
        }

        void failNextSubscribes(final int count) {
            failures.set(count);
        }

        int subscribeCount() {
            return startPositions.size();
        }

        List<Long> startPositions() {
            return List.copyOf(startPositions);
        }

        List<Subscription> unsubscribed() {
            return List.copyOf(unsubscribed);
        }

        Subscription currentSubscription() {
            return current;
        }

        void awaitSubscribeCount(final int expected) throws InterruptedException {
            final long deadline = System.currentTimeMillis() + 5_000;
            while (subscribeCount() < expected && System.currentTimeMillis() < deadline) {
                Thread.sleep(5);
            }
            assertThat(subscribeCount()).isEqualTo(expected);
        }

    }

    /**
     * Subscription of the {@link FakeStore}.
     */
    private static final class FakeSubscription extends Subscription {

        private static final long serialVersionUID = 1000L;

        private FakeSubscription(final StreamId streamId) {
            super(streamId, null);
        }

    }

}
