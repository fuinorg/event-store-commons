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
package org.fuin.esc.pg;

import org.fuin.esc.spi.WakeupSource;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * PostgreSQL {@code LISTEN/NOTIFY} {@link WakeupSource} (ESC-2 rung 2). It runs {@code LISTEN} on a
 * dedicated JDBC connection and, on a background daemon thread, blocks on
 * {@link PGConnection#getNotifications(int)} - which returns <em>early</em> as soon as a {@code NOTIFY}
 * arrives - firing the catch-up callback with "read now" latency. If no notification arrives within the
 * configured poll interval it fires anyway, so the wake-up doubles as a poll safety-net: a {@code NOTIFY}
 * missed while no listener was connected (PostgreSQL notifications are at-most-once and non-durable) is still
 * picked up on the next interval, and the {@code CheckpointStore} guarantees no events are skipped. Correctness
 * therefore never depends on the notification; it only cuts latency.
 * <p>
 * The {@code connection} is owned by the caller and is <em>not</em> closed by {@link #close()} (only the
 * background thread and the {@code LISTEN} registration are released). The connection must be dedicated to this
 * source for its lifetime (a listening connection blocks on notifications and must not be shared).
 */
@ThreadSafe
public final class PgListenNotifyWakeupSource implements WakeupSource {

    private static final Logger LOG = LoggerFactory.getLogger(PgListenNotifyWakeupSource.class);

    /** Upper bound for a single {@code getNotifications} block, so {@link #close()} stays responsive even
     * when the configured poll interval is large. */
    private static final int MAX_SLICE_MILLIS = 1000;

    private final Connection connection;

    private final String channel;

    private final int pollTimeoutMillis;

    private volatile boolean running;

    @Nullable
    private Thread thread;

    /**
     * Constructor with all mandatory data.
     *
     * @param connection  Dedicated JDBC connection used to {@code LISTEN} (owned by the caller, not closed by
     *                    {@link #close()}).
     * @param channel     PostgreSQL notification channel to listen on. Must be a simple SQL identifier
     *                    ({@code [A-Za-z_][A-Za-z0-9_]*}) and match the channel used by the {@code NOTIFY}
     *                    (see {@link PgNotify}).
     * @param pollTimeout Poll safety-net interval: the callback fires at least this often even without a
     *                    notification. Also bounds worst-case {@link #close()} latency together with the
     *                    internal slice. Must be {@code >= 1}.
     * @param unit        Time unit of {@code pollTimeout}.
     */
    public PgListenNotifyWakeupSource(final Connection connection, final String channel, final long pollTimeout,
                                      final TimeUnit unit) {
        super();
        Contract.requireArgNotNull("connection", connection);
        Contract.requireArgNotNull("channel", channel);
        Contract.requireArgNotNull("unit", unit);
        PgNotify.requireIdentifier("channel", channel);
        Contract.requireArgMin("pollTimeout", pollTimeout, 1);
        final long millis = unit.toMillis(pollTimeout);
        this.connection = connection;
        this.channel = channel;
        this.pollTimeoutMillis = (int) Math.min(millis, Integer.MAX_VALUE);
    }

    @Override
    public synchronized void start(final Runnable onWakeup) {
        Contract.requireArgNotNull("onWakeup", onWakeup);
        if (thread != null) {
            throw new IllegalStateException("start(..) must only be called once");
        }
        try (Statement st = connection.createStatement()) {
            st.execute("LISTEN \"" + channel + "\"");
        } catch (final SQLException ex) {
            throw new RuntimeException("Failed to LISTEN on channel '" + channel + "'", ex);
        }
        running = true;
        final Thread t = new Thread(() -> runLoop(onWakeup), "esc-pg-listen-" + channel);
        t.setDaemon(true);
        thread = t;
        t.start();
    }

    private void runLoop(final Runnable onWakeup) {
        // Initial catch-up so an existing backlog is processed without waiting for the first notification.
        safeRun(onWakeup);
        long remaining = pollTimeoutMillis;
        while (running) {
            final int slice = (int) Math.min(remaining, MAX_SLICE_MILLIS);
            final boolean notified;
            try {
                final PGNotification[] notifications = connection.unwrap(PGConnection.class).getNotifications(slice);
                notified = (notifications != null) && (notifications.length > 0);
            } catch (final SQLException ex) {
                if (running) {
                    LOG.error("Error while waiting for notifications on channel '{}'", channel, ex);
                    if (!sleepQuietly(slice)) {
                        return;
                    }
                }
                continue;
            }
            if (!running) {
                return;
            }
            if (notified) {
                safeRun(onWakeup);
                remaining = pollTimeoutMillis;
            } else {
                remaining -= slice;
                if (remaining <= 0) {
                    safeRun(onWakeup);
                    remaining = pollTimeoutMillis;
                }
            }
        }
    }

    private void safeRun(final Runnable onWakeup) {
        try {
            onWakeup.run();
        } catch (final RuntimeException ex) {
            LOG.error("Wake-up callback failed for channel '{}'", channel, ex);
        }
    }

    private boolean sleepQuietly(final int millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        final Thread t = thread;
        if (t == null) {
            return;
        }
        thread = null;
        t.interrupt();
        try {
            t.join(TimeUnit.SECONDS.toMillis(5));
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        try (Statement st = connection.createStatement()) {
            st.execute("UNLISTEN \"" + channel + "\"");
        } catch (final SQLException ex) {
            LOG.warn("Failed to UNLISTEN channel '{}' on close", channel, ex);
        }
    }

}
