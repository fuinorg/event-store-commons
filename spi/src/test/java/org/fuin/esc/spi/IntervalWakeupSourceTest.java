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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link IntervalWakeupSource} class.
 */
public class IntervalWakeupSourceTest {

    @Test
    public void testFiresRepeatedly() throws InterruptedException {

        // PREPARE
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            final CountDownLatch latch = new CountDownLatch(3);
            final IntervalWakeupSource testee = new IntervalWakeupSource(scheduler, 0, 20, TimeUnit.MILLISECONDS);

            // TEST
            testee.start(latch::countDown);

            // VERIFY
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            testee.close();
        } finally {
            scheduler.shutdownNow();
        }

    }

    @Test
    public void testStartOnlyOnce() {

        // PREPARE
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            final IntervalWakeupSource testee = new IntervalWakeupSource(scheduler, 0, 1, TimeUnit.HOURS);
            testee.start(() -> {
            });

            // TEST & VERIFY
            assertThatThrownBy(() -> testee.start(() -> {
            })).isInstanceOf(IllegalStateException.class);
            testee.close();
        } finally {
            scheduler.shutdownNow();
        }

    }

}
