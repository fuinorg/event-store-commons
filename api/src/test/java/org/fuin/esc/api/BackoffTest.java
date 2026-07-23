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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link Backoff}.
 */
public final class BackoffTest {

    private static final Backoff TESTEE = new Backoff(Duration.ofMillis(100), Duration.ofMillis(800), 2.0, 0.0, 5);

    @Test
    public void testBaseDelayGrowsExponentially() {
        assertThat(TESTEE.baseDelay(1)).isEqualTo(Duration.ofMillis(100));
        assertThat(TESTEE.baseDelay(2)).isEqualTo(Duration.ofMillis(200));
        assertThat(TESTEE.baseDelay(3)).isEqualTo(Duration.ofMillis(400));
        assertThat(TESTEE.baseDelay(4)).isEqualTo(Duration.ofMillis(800));
    }

    @Test
    public void testBaseDelayIsCapped() {
        // Without the cap a long outage would keep a consumer down long after the store returned.
        assertThat(TESTEE.baseDelay(5)).isEqualTo(Duration.ofMillis(800));
        assertThat(TESTEE.baseDelay(50)).isEqualTo(Duration.ofMillis(800));
        assertThat(TESTEE.baseDelay(5_000)).isEqualTo(Duration.ofMillis(800));
    }

    @Test
    public void testBaseDelayRejectsAttemptBelowOne() {
        assertThatThrownBy(() -> TESTEE.baseDelay(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testDelayWithoutJitterIsTheBaseDelay() {
        assertThat(TESTEE.delay(3)).isEqualTo(Duration.ofMillis(400));
    }

    @Test
    public void testDelayWithJitterStaysWithinBounds() {
        final Backoff jittered = new Backoff(Duration.ofMillis(100), Duration.ofMillis(800), 2.0, 0.5, 5);

        for (int i = 0; i < 1_000; i++) {
            // 50% jitter on a base delay of 400 ms spreads the wait over [200 ms, 400 ms].
            assertThat(jittered.delay(3)).isBetween(Duration.ofMillis(200), Duration.ofMillis(400));
        }
    }

    @Test
    public void testDelayWithFullJitterMayShrinkToZero() {
        final Backoff jittered = new Backoff(Duration.ofMillis(100), Duration.ofMillis(800), 2.0, 1.0, 5);

        for (int i = 0; i < 1_000; i++) {
            assertThat(jittered.delay(3)).isBetween(Duration.ZERO, Duration.ofMillis(400));
        }
    }

    @Test
    public void testAllowsAttempt() {
        assertThat(TESTEE.allowsAttempt(1)).isTrue();
        assertThat(TESTEE.allowsAttempt(5)).isTrue();
        assertThat(TESTEE.allowsAttempt(6)).isFalse();
    }

    @Test
    public void testAllowsAttemptWithoutLimit() {
        final Backoff unlimited = TESTEE.withMaxAttempts(Backoff.UNLIMITED_ATTEMPTS);

        assertThat(unlimited.allowsAttempt(1)).isTrue();
        assertThat(unlimited.allowsAttempt(Integer.MAX_VALUE)).isTrue();
    }

    @Test
    public void testDefault() {
        assertThat(Backoff.DEFAULT.initialDelay()).isEqualTo(Duration.ofMillis(500));
        assertThat(Backoff.DEFAULT.maxDelay()).isEqualTo(Duration.ofSeconds(30));
        assertThat(Backoff.DEFAULT.maxAttempts()).isEqualTo(Backoff.UNLIMITED_ATTEMPTS);
    }

    @Test
    public void testInvalidArguments() {
        assertThatThrownBy(() -> new Backoff(null, Duration.ofSeconds(1), 2.0, 0.5, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Backoff(Duration.ofSeconds(1), null, 2.0, 0.5, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Backoff(Duration.ZERO, Duration.ofSeconds(1), 2.0, 0.5, 3))
                .isInstanceOf(IllegalArgumentException.class);
        // A maximum below the initial delay would silently shorten the very first wait.
        assertThatThrownBy(() -> new Backoff(Duration.ofSeconds(2), Duration.ofSeconds(1), 2.0, 0.5, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Backoff(Duration.ofMillis(100), Duration.ofSeconds(1), 0.9, 0.5, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Backoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2.0, 1.5, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Backoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2.0, 0.5, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
