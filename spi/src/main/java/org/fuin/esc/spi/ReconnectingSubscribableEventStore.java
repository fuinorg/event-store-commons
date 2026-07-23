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
import org.fuin.esc.api.EventStoreCapabilities;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.SubscribableEventStoreAsync;
import org.fuin.esc.api.Subscription;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Backend-neutral decorator that re-establishes a dropped subscription with exponential backoff and jitter,
 * so a consumer survives a store restart or a network blip without implementing its own retry loop.
 * <p>
 * The underlying stores deliver a drop to {@code onDrop} and then stay silent - the subscription is gone for
 * good. This decorator keeps the {@link Subscription} handle it returned alive across reconnects instead:
 * the consumer's callbacks always see that same handle, and {@link #unsubscribeFromStream(Subscription)} on
 * it stops both the current inner subscription and any pending reconnect.
 * <p>
 * <b>Resume position.</b> Neither {@link CommonEvent} nor {@link Subscription} carries a stream position, so
 * the decorator counts what it delivered and resumes at {@code eventNumber + delivered}. This is exact for a
 * subscription that started at an absolute event number. A subscription started with
 * {@link EscApiUtils#SUBSCRIBE_TO_NEW_EVENTS} has no absolute anchor and is re-established as "new events"
 * again, so events written during the outage are <em>not</em> redelivered - which is the right behaviour for
 * a wake-up subscription (the consumer's catch-up pass reads them from its checkpoint) but must not be
 * mistaken for gap-free delivery.
 * <p>
 * <b>Delivery is at-least-once.</b> The delivered count is advanced only after the consumer's {@code onEvent}
 * returns; an event whose handler threw is redelivered after a reconnect.
 * <p>
 * <b>The initial subscribe is not retried.</b> A failure there fails the returned future so the caller sees
 * why its wiring did not come up; reconnection applies to a subscription that was once established. The
 * scheduler is supplied and owned by the caller - {@link #close()} stops reconnecting but never shuts it
 * down.
 */
@ThreadSafe
public final class ReconnectingSubscribableEventStore implements SubscribableEventStoreAsync {

    private static final Logger LOG = LoggerFactory.getLogger(ReconnectingSubscribableEventStore.class);

    private final SubscribableEventStoreAsync delegate;

    private final ScheduledExecutorService scheduler;

    private final Backoff backoff;

    /**
     * Constructor with all mandatory data.
     *
     * @param delegate  Store that actually holds the subscriptions.
     * @param scheduler Executor used to re-subscribe after a drop (owned by the caller).
     * @param backoff   Delay schedule between reconnect attempts.
     */
    public ReconnectingSubscribableEventStore(final SubscribableEventStoreAsync delegate,
                                              final ScheduledExecutorService scheduler,
                                              final Backoff backoff) {
        super();
        Contract.requireArgNotNull("delegate", delegate);
        Contract.requireArgNotNull("scheduler", scheduler);
        Contract.requireArgNotNull("backoff", backoff);
        this.delegate = delegate;
        this.scheduler = scheduler;
        this.backoff = backoff;
    }

    @Override
    public CompletableFuture<Void> open() {
        return delegate.open();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public EventStoreCapabilities capabilities() {
        return delegate.capabilities();
    }

    @Override
    public CompletableFuture<Subscription> subscribeToStream(final StreamId streamId, final long eventNumber,
                                                             final BiConsumer<Subscription, CommonEvent> onEvent,
                                                             final BiConsumer<Subscription, Exception> onDrop) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("onEvent", onEvent);
        Contract.requireArgNotNull("onDrop", onDrop);

        final ReconnectingSubscription handle = new ReconnectingSubscription(streamId, eventNumber);
        return attach(handle, onEvent, onDrop).thenApply(inner -> handle);
    }

    /**
     * Subscribes to the delegate on behalf of the given handle, resuming at the position the handle reached.
     *
     * @param handle  Stable subscription handed to the consumer.
     * @param onEvent Consumer callback for an event.
     * @param onDrop  Consumer callback for a drop that could not be repaired.
     * @return Future with the inner subscription.
     */
    private CompletableFuture<Subscription> attach(final ReconnectingSubscription handle,
                                                   final BiConsumer<Subscription, CommonEvent> onEvent,
                                                   final BiConsumer<Subscription, Exception> onDrop) {

        final BiConsumer<Subscription, CommonEvent> innerOnEvent = (inner, event) -> {
            onEvent.accept(handle, event);
            // Only after the consumer returned - an event whose handler threw is redelivered on reconnect.
            handle.eventDelivered();
        };
        final BiConsumer<Subscription, Exception> innerOnDrop = (inner, ex) -> {
            if (handle.isClosed()) {
                return;
            }
            scheduleReconnect(handle, onEvent, onDrop, ex);
        };

        return delegate.subscribeToStream(handle.getStreamId(), handle.resumeFrom(), innerOnEvent, innerOnDrop)
                .thenApply(inner -> {
                    handle.attached(inner);
                    if (handle.isClosed()) {
                        // Raced with unsubscribeFromStream(..) - tear the fresh subscription down again.
                        delegate.unsubscribeFromStream(inner);
                    }
                    return inner;
                });
    }

    /**
     * Schedules the next reconnect attempt, or gives up and tells the consumer.
     *
     * @param handle  Stable subscription handed to the consumer.
     * @param onEvent Consumer callback for an event.
     * @param onDrop  Consumer callback for a drop that could not be repaired.
     * @param cause   Failure that ended the previous subscription.
     */
    private void scheduleReconnect(final ReconnectingSubscription handle,
                                   final BiConsumer<Subscription, CommonEvent> onEvent,
                                   final BiConsumer<Subscription, Exception> onDrop,
                                   final Exception cause) {

        final int attempt = handle.nextAttempt();
        if (!backoff.allowsAttempt(attempt)) {
            LOG.warn("Subscription to stream '{}' dropped and could not be re-established within {} attempts, "
                    + "giving up", handle.getStreamId().asString(), backoff.maxAttempts(), cause);
            handle.markClosed();
            onDrop.accept(handle, cause);
            return;
        }
        final long delayMillis = backoff.delay(attempt).toMillis();
        LOG.debug("Subscription to stream '{}' dropped, reconnect attempt {} in {} ms: {}",
                handle.getStreamId().asString(), attempt, delayMillis, cause);
        try {
            scheduler.schedule(() -> reconnect(handle, onEvent, onDrop), delayMillis, TimeUnit.MILLISECONDS);
        } catch (final RejectedExecutionException ex) {
            // Scheduler already shut down (the application is closing) - nothing left to do.
            LOG.trace("Reconnect rejected (shutting down) for stream '{}'", handle.getStreamId().asString());
            handle.markClosed();
        }
    }

    /**
     * Performs one reconnect attempt and schedules the next one if it fails.
     *
     * @param handle  Stable subscription handed to the consumer.
     * @param onEvent Consumer callback for an event.
     * @param onDrop  Consumer callback for a drop that could not be repaired.
     */
    private void reconnect(final ReconnectingSubscription handle,
                           final BiConsumer<Subscription, CommonEvent> onEvent,
                           final BiConsumer<Subscription, Exception> onDrop) {
        if (handle.isClosed()) {
            return;
        }
        attach(handle, onEvent, onDrop).whenComplete((inner, ex) -> {
            if (ex == null) {
                LOG.info("Subscription to stream '{}' re-established at event number {}",
                        handle.getStreamId().asString(), handle.resumeFrom());
                handle.attemptSucceeded();
            } else if (!handle.isClosed()) {
                scheduleReconnect(handle, onEvent, onDrop, asException(ex));
            }
        });
    }

    @Override
    public CompletableFuture<Void> unsubscribeFromStream(final Subscription subscription) {
        Contract.requireArgNotNull("subscription", subscription);
        if (!(subscription instanceof ReconnectingSubscription handle)) {
            return delegate.unsubscribeFromStream(subscription);
        }
        handle.markClosed();
        final Subscription inner = handle.current();
        if (inner == null) {
            // Dropped and not yet re-established - the pending attempt sees the closed flag and stops.
            return CompletableFuture.completedFuture(null);
        }
        return delegate.unsubscribeFromStream(inner);
    }

    private static Exception asException(final Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null
                && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        if (cause instanceof Exception ex) {
            return ex;
        }
        return new IllegalStateException("Could not re-establish the subscription", cause);
    }

}
