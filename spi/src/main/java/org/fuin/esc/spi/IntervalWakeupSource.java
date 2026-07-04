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

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Portable default {@link WakeupSource}: fires the callback on a fixed interval using a caller-provided
 * {@link ScheduledExecutorService}. Uses {@code scheduleWithFixedDelay}, so a new pass only starts after the
 * previous one finished plus the interval - runs never overlap. The scheduler is owned by the caller and is
 * <em>not</em> shut down by {@link #close()}.
 */
@ThreadSafe
public final class IntervalWakeupSource implements WakeupSource {

    private final ScheduledExecutorService scheduler;

    private final long initialDelay;

    private final long interval;

    private final TimeUnit unit;

    @Nullable
    private volatile ScheduledFuture<?> future;

    /**
     * Constructor with all mandatory data.
     *
     * @param scheduler    Executor used to run the periodic callback (owned by the caller).
     * @param initialDelay Delay before the first invocation.
     * @param interval     Delay between the end of one invocation and the start of the next.
     * @param unit         Time unit of {@code initialDelay} and {@code interval}.
     */
    public IntervalWakeupSource(final ScheduledExecutorService scheduler, final long initialDelay,
                                final long interval, final TimeUnit unit) {
        super();
        Contract.requireArgNotNull("scheduler", scheduler);
        Contract.requireArgMin("initialDelay", initialDelay, 0);
        Contract.requireArgMin("interval", interval, 1);
        Contract.requireArgNotNull("unit", unit);
        this.scheduler = scheduler;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.unit = unit;
    }

    @Override
    public synchronized void start(final Runnable onWakeup) {
        Contract.requireArgNotNull("onWakeup", onWakeup);
        if (future != null) {
            throw new IllegalStateException("start(..) must only be called once");
        }
        future = scheduler.scheduleWithFixedDelay(onWakeup, initialDelay, interval, unit);
    }

    @Override
    public synchronized void close() {
        final ScheduledFuture<?> f = future;
        if (f != null) {
            f.cancel(false);
            future = null;
        }
    }

}
