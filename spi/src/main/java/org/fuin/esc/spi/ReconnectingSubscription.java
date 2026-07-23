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
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.Subscription;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Subscription handle of the {@link ReconnectingSubscribableEventStore} that stays valid while the
 * subscription underneath it is torn down and re-established. The consumer holds this instance; the inner
 * subscription it currently points at is an implementation detail that changes on every reconnect.
 * <p>
 * In contrast to the other {@link Subscription} implementations this one therefore carries mutable state.
 * It is kept consistent with atomics rather than locks because it is written from the store's callback
 * thread and read from the reconnect scheduler. The inherited immutable identity (stream and event number)
 * is what survives serialization; the live state is transient, as a deserialized handle is no longer
 * attached to anything.
 */
@ThreadSafe
final class ReconnectingSubscription extends Subscription {

    @Serial
    private static final long serialVersionUID = 1000L;

    private final long startEventNumber;

    private final transient AtomicReference<@Nullable Subscription> current = new AtomicReference<>();

    private final transient AtomicLong delivered = new AtomicLong();

    private final transient AtomicInteger attempt = new AtomicInteger();

    private final transient AtomicBoolean closed = new AtomicBoolean();

    /**
     * Constructor with all mandatory data.
     *
     * @param streamId         Stream this subscription follows.
     * @param startEventNumber Event number the consumer originally subscribed from.
     */
    ReconnectingSubscription(final StreamId streamId, final long startEventNumber) {
        super(streamId, null);
        this.startEventNumber = startEventNumber;
    }

    /**
     * Returns the event number a (re-)subscribe should start at.
     *
     * @return Absolute event number, or {@link EscApiUtils#SUBSCRIBE_TO_NEW_EVENTS} if the consumer
     *         subscribed to new events only - that has no absolute anchor to resume from.
     */
    long resumeFrom() {
        if (startEventNumber == EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS) {
            return EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS;
        }
        return startEventNumber + delivered.get();
    }

    /**
     * Records that the consumer accepted one more event, which moves the resume position forward.
     */
    void eventDelivered() {
        delivered.incrementAndGet();
    }

    /**
     * Points this handle at the subscription that currently backs it.
     *
     * @param subscription Inner subscription.
     */
    void attached(final Subscription subscription) {
        current.set(subscription);
    }

    /**
     * Returns the subscription that currently backs this handle.
     *
     * @return Inner subscription or {@literal null} while a reconnect is pending.
     */
    @Nullable
    Subscription current() {
        return current.get();
    }

    /**
     * Counts and returns the number of the reconnect attempt that is about to be made.
     *
     * @return Attempt number, starting at 1 after the first drop.
     */
    int nextAttempt() {
        current.set(null);
        return attempt.incrementAndGet();
    }

    /**
     * Resets the attempt counter after a successful reconnect, so a later outage gets the full schedule
     * again instead of continuing at the delay the previous one ended with.
     */
    void attemptSucceeded() {
        attempt.set(0);
    }

    /**
     * Marks this handle as closed, which stops any further reconnect.
     */
    void markClosed() {
        closed.set(true);
    }

    /**
     * Determines whether this handle was closed.
     *
     * @return {@literal true} if no further reconnect should happen.
     */
    boolean isClosed() {
        return closed.get();
    }

}
