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
package org.fuin.esc.api;

import org.fuin.objects4j.common.Immutable;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Delay schedule for retrying an operation that failed transiently: the delay grows exponentially with the
 * number of consecutive failures, is capped at a maximum, and carries jitter.
 * <p>
 * The cap matters because an unbounded exponential quickly turns a short outage into a long one - a consumer
 * that reached a delay of ten minutes stays down for ten minutes after the store is back. The jitter matters
 * because without it every consumer that lost the same store retries in lockstep and hits it again as one
 * burst the moment it returns.
 * <p>
 * This type only computes delays; it neither sleeps nor schedules. That keeps it usable both from a blocking
 * retry loop and from a {@link java.util.concurrent.ScheduledExecutorService}.
 *
 * @param initialDelay Delay after the first failure.
 * @param maxDelay     Upper bound for the delay, however many failures accumulate.
 * @param multiplier   Factor the delay is multiplied with after each further failure ({@literal >= 1}).
 * @param jitterFactor Fraction of the computed delay that is randomized, between 0 (no jitter) and 1 (the
 *                     delay may shrink to zero). A value of {@code 0.5} spreads the delay over
 *                     {@code [0.5 * delay, delay]}.
 * @param maxAttempts  Maximum number of attempts, or {@link #UNLIMITED_ATTEMPTS} to keep retrying forever.
 */
@Immutable
public record Backoff(Duration initialDelay, Duration maxDelay, double multiplier, double jitterFactor,
                      int maxAttempts) {

    /** Value for {@link #maxAttempts()} that lets an operation be retried without a limit. */
    public static final int UNLIMITED_ATTEMPTS = -1;

    /**
     * Default schedule: 500 ms growing by a factor of 2 up to 30 s, with 50% jitter and no attempt limit.
     * Suitable for re-establishing a long-lived connection, where giving up is worse than waiting.
     */
    public static final Backoff DEFAULT = new Backoff(Duration.ofMillis(500), Duration.ofSeconds(30), 2.0, 0.5,
            UNLIMITED_ATTEMPTS);

    /**
     * Constructor with all data.
     *
     * @param initialDelay Delay after the first failure.
     * @param maxDelay     Upper bound for the delay.
     * @param multiplier   Factor the delay is multiplied with after each further failure.
     * @param jitterFactor Fraction of the computed delay that is randomized.
     * @param maxAttempts  Maximum number of attempts or {@link #UNLIMITED_ATTEMPTS}.
     */
    public Backoff {
        if (initialDelay == null) {
            throw new IllegalArgumentException("initialDelay cannot be null");
        }
        if (maxDelay == null) {
            throw new IllegalArgumentException("maxDelay cannot be null");
        }
        if (initialDelay.isNegative() || initialDelay.isZero()) {
            throw new IllegalArgumentException("initialDelay must be positive, but was: " + initialDelay);
        }
        if (maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException(
                    "maxDelay must not be shorter than initialDelay (" + initialDelay + "), but was: " + maxDelay);
        }
        if (multiplier < 1.0) {
            throw new IllegalArgumentException("multiplier must be at least 1.0, but was: " + multiplier);
        }
        if (jitterFactor < 0.0 || jitterFactor > 1.0) {
            throw new IllegalArgumentException("jitterFactor must be between 0.0 and 1.0, but was: " + jitterFactor);
        }
        if (maxAttempts != UNLIMITED_ATTEMPTS && maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "maxAttempts must be at least 1 or UNLIMITED_ATTEMPTS, but was: " + maxAttempts);
        }
    }

    /**
     * Returns the delay before the given attempt, without jitter. Useful to reason about (and to test) the
     * schedule; use {@link #delay(int)} to actually wait.
     *
     * @param attempt Number of the attempt that is about to be made, starting at 1 for the first retry.
     * @return Delay before that attempt, capped at {@link #maxDelay()}.
     */
    public Duration baseDelay(final int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be at least 1, but was: " + attempt);
        }
        double millis = initialDelay.toMillis();
        final double cap = maxDelay.toMillis();
        for (int i = 1; i < attempt && millis < cap; i++) {
            millis = millis * multiplier;
        }
        return Duration.ofMillis((long) Math.min(millis, cap));
    }

    /**
     * Returns the delay before the given attempt with jitter applied, which is what a caller should wait.
     *
     * @param attempt Number of the attempt that is about to be made, starting at 1 for the first retry.
     * @return Randomized delay in {@code [(1 - jitterFactor) * baseDelay, baseDelay]}.
     */
    public Duration delay(final int attempt) {
        final long base = baseDelay(attempt).toMillis();
        if (jitterFactor == 0.0) {
            return Duration.ofMillis(base);
        }
        final long lowest = (long) (base * (1.0 - jitterFactor));
        if (lowest >= base) {
            return Duration.ofMillis(base);
        }
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(lowest, base + 1));
    }

    /**
     * Determines whether another attempt is allowed.
     *
     * @param attempt Number of the attempt that is about to be made, starting at 1 for the first retry.
     * @return {@literal true} if the attempt is still within {@link #maxAttempts()}.
     */
    public boolean allowsAttempt(final int attempt) {
        return maxAttempts == UNLIMITED_ATTEMPTS || attempt <= maxAttempts;
    }

    /**
     * Returns a copy of this schedule that gives up after the given number of attempts. Convenience for the
     * bounded case, where waiting forever is not an option.
     *
     * @param attempts Maximum number of attempts or {@link #UNLIMITED_ATTEMPTS}.
     * @return New instance.
     */
    public Backoff withMaxAttempts(final int attempts) {
        return new Backoff(initialDelay, maxDelay, multiplier, jitterFactor, attempts);
    }

}
