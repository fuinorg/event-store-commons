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

import org.fuin.esc.api.EscApiUtils;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.Subscription;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ReconnectingSubscription}.
 */
public final class ReconnectingSubscriptionTest {

    private static final StreamId STREAM = new SimpleStreamId("MyStream");

    @Test
    public void testResumeFromCountsDeliveredEvents() {

        // PREPARE
        final ReconnectingSubscription testee = new ReconnectingSubscription(STREAM, 10);
        assertThat(testee.resumeFrom()).isEqualTo(10);

        // TEST
        testee.eventDelivered();
        testee.eventDelivered();
        testee.eventDelivered();

        // VERIFY
        assertThat(testee.resumeFrom()).isEqualTo(13);
    }

    @Test
    public void testResumeFromStaysOnNewEventsWithoutAnAnchor() {

        // PREPARE: "new events" has no absolute position to count from
        final ReconnectingSubscription testee =
                new ReconnectingSubscription(STREAM, EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS);

        // TEST
        testee.eventDelivered();
        testee.eventDelivered();

        // VERIFY
        assertThat(testee.resumeFrom()).isEqualTo(EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS);
    }

    @Test
    public void testAttemptCounterResetsAfterASuccessfulReconnect() {

        // PREPARE: an outage that took three attempts to repair
        final ReconnectingSubscription testee = new ReconnectingSubscription(STREAM, 0);
        assertThat(testee.nextAttempt()).isEqualTo(1);
        assertThat(testee.nextAttempt()).isEqualTo(2);
        assertThat(testee.nextAttempt()).isEqualTo(3);

        // TEST
        testee.attemptSucceeded();

        // VERIFY: a later outage gets the full schedule again, not the delay this one ended with
        assertThat(testee.nextAttempt()).isEqualTo(1);
    }

    @Test
    public void testNextAttemptDetachesTheInnerSubscription() {

        // PREPARE
        final ReconnectingSubscription testee = new ReconnectingSubscription(STREAM, 0);
        final Subscription inner = new FakeSubscription(STREAM);
        testee.attached(inner);
        assertThat(testee.current()).isSameAs(inner);

        // TEST: the subscription is gone once a reconnect is pending
        testee.nextAttempt();

        // VERIFY
        assertThat(testee.current()).isNull();
    }

    @Test
    public void testClosed() {

        // PREPARE
        final ReconnectingSubscription testee = new ReconnectingSubscription(STREAM, 0);
        assertThat(testee.isClosed()).isFalse();

        // TEST
        testee.markClosed();

        // VERIFY
        assertThat(testee.isClosed()).isTrue();
    }

    @Test
    public void testStreamIdIsTheImmutableIdentity() {
        assertThat(new ReconnectingSubscription(STREAM, 0).getStreamId()).isEqualTo(STREAM);
    }

    /**
     * Stand-in for the subscription an event store would hand out.
     */
    private static final class FakeSubscription extends Subscription {

        private static final long serialVersionUID = 1000L;

        private FakeSubscription(final StreamId streamId) {
            super(streamId, null);
        }

    }

}
