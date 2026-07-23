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

import org.fuin.esc.api.Backoff;
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
 * <b>A dropped listen connection does not stop the wake-ups.</b> Because correctness rests on the poll and not
 * on the notification, losing the connection may only cost latency: the source keeps firing the callback every
 * poll interval while the channel is down - the consumer's catch-up pass runs on its own connection and is
 * unaffected - and, when constructed with a {@link PgListenConnectionFactory}, re-opens the connection and
 * re-issues the {@code LISTEN} with exponential backoff and jitter. Every successful reconnect fires the
 * callback at once, because notifications sent while nothing was listening are gone for good. Constructed with
 * a plain {@link Connection} there is nothing to reconnect with, so the source stays in the degraded
 * poll-only mode after a drop.
 * <p>
 * A caller-supplied {@code connection} is owned by the caller and is <em>not</em> closed by {@link #close()}
 * (only the background thread and the {@code LISTEN} registration are released); connections this source
 * opened through a {@link PgListenConnectionFactory} are owned and closed by it. The connection must be
 * dedicated to this source for its lifetime (a listening connection blocks on notifications and must not be
 * shared).
 */
@ThreadSafe
public final class PgListenNotifyWakeupSource implements WakeupSource {

    private static final Logger LOG = LoggerFactory.getLogger(PgListenNotifyWakeupSource.class);

    /** Upper bound for a single {@code getNotifications} block or sleep, so {@link #close()} stays responsive
     * even when the configured poll interval or reconnect delay is large. */
    private static final int MAX_SLICE_MILLIS = 1000;

    @Nullable
    private final PgListenConnectionFactory connectionFactory;

    private final boolean ownsConnection;

    private final String channel;

    private final int pollTimeoutMillis;

    private final Backoff reconnectBackoff;

    @Nullable
    private volatile Connection connection;

    private volatile boolean running;

    @Nullable
    private Thread thread;

    /**
     * Constructor for a caller-owned connection. A dropped connection cannot be replaced, so the source
     * degrades to the poll safety-net; use {@link #PgListenNotifyWakeupSource(PgListenConnectionFactory,
     * String, long, TimeUnit)} to get the notification latency back after an outage.
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
        this(connection, null, channel, pollTimeout, unit, Backoff.DEFAULT);
        Contract.requireArgNotNull("connection", connection);
    }

    /**
     * Constructor that can replace a dropped connection, using {@link Backoff#DEFAULT}.
     *
     * @param connectionFactory Opens the dedicated JDBC connection used to {@code LISTEN}. Connections it
     *                          returns are owned and closed by this source.
     * @param channel           PostgreSQL notification channel to listen on.
     * @param pollTimeout       Poll safety-net interval.
     * @param unit              Time unit of {@code pollTimeout}.
     */
    public PgListenNotifyWakeupSource(final PgListenConnectionFactory connectionFactory, final String channel,
                                      final long pollTimeout, final TimeUnit unit) {
        this(null, connectionFactory, channel, pollTimeout, unit, Backoff.DEFAULT);
        Contract.requireArgNotNull("connectionFactory", connectionFactory);
    }

    /**
     * Constructor that can replace a dropped connection, with an explicit reconnect schedule.
     *
     * @param connectionFactory Opens the dedicated JDBC connection used to {@code LISTEN}. Connections it
     *                          returns are owned and closed by this source.
     * @param channel           PostgreSQL notification channel to listen on.
     * @param pollTimeout       Poll safety-net interval.
     * @param unit              Time unit of {@code pollTimeout}.
     * @param reconnectBackoff  Delay schedule between reconnect attempts. Once its attempts are used up the
     *                          source stops reconnecting and continues as a plain poll.
     */
    public PgListenNotifyWakeupSource(final PgListenConnectionFactory connectionFactory, final String channel,
                                      final long pollTimeout, final TimeUnit unit,
                                      final Backoff reconnectBackoff) {
        this(null, connectionFactory, channel, pollTimeout, unit, reconnectBackoff);
        Contract.requireArgNotNull("connectionFactory", connectionFactory);
    }

    private PgListenNotifyWakeupSource(@Nullable final Connection connection,
                                       @Nullable final PgListenConnectionFactory connectionFactory,
                                       final String channel, final long pollTimeout, final TimeUnit unit,
                                       final Backoff reconnectBackoff) {
        super();
        Contract.requireArgNotNull("channel", channel);
        Contract.requireArgNotNull("unit", unit);
        Contract.requireArgNotNull("reconnectBackoff", reconnectBackoff);
        PgNotify.requireIdentifier("channel", channel);
        Contract.requireArgMin("pollTimeout", pollTimeout, 1);
        final long millis = unit.toMillis(pollTimeout);
        this.connection = connection;
        this.connectionFactory = connectionFactory;
        this.ownsConnection = connectionFactory != null;
        this.channel = channel;
        this.pollTimeoutMillis = (int) Math.min(millis, Integer.MAX_VALUE);
        this.reconnectBackoff = reconnectBackoff;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The <em>first</em> connection is deliberately not retried: a store that cannot be reached at wiring
     * time is a startup problem the caller should see. Only a connection that was once established is
     * re-opened by the background thread.
     */
    @Override
    public synchronized void start(final Runnable onWakeup) {
        Contract.requireArgNotNull("onWakeup", onWakeup);
        if (thread != null) {
            throw new IllegalStateException("start(..) must only be called once");
        }
        Connection conn = connection;
        if (conn == null) {
            try {
                conn = requireFactory().create();
            } catch (final SQLException ex) {
                throw new RuntimeException("Failed to open the listen connection for channel '" + channel + "'", ex);
            }
            connection = conn;
        }
        try {
            listen(conn);
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
        long nextPollAt = System.currentTimeMillis() + pollTimeoutMillis;
        long nextReconnectAt = System.currentTimeMillis();
        int attempt = 0;
        boolean reconnecting = connectionFactory != null;

        while (running) {
            final long now = System.currentTimeMillis();
            final Connection conn = connection;

            if (conn != null) {
                final int slice = (int) clamp(nextPollAt - now, 1, MAX_SLICE_MILLIS);
                final boolean notified;
                try {
                    final PGNotification[] notifications = conn.unwrap(PGConnection.class).getNotifications(slice);
                    notified = (notifications != null) && (notifications.length > 0);
                } catch (final SQLException ex) {
                    if (running) {
                        LOG.warn("Lost the listen connection for channel '{}' - falling back to a plain poll "
                                + "every {} ms", channel, pollTimeoutMillis, ex);
                    }
                    closeQuietly(conn);
                    connection = null;
                    nextReconnectAt = System.currentTimeMillis();
                    attempt = 0;
                    continue;
                }
                if (!running) {
                    return;
                }
                if (notified || System.currentTimeMillis() >= nextPollAt) {
                    safeRun(onWakeup);
                    nextPollAt = System.currentTimeMillis() + pollTimeoutMillis;
                }
                continue;
            }

            // Degraded: no notification channel, so the wake-up is a plain poll until a reconnect succeeds.
            if (reconnecting && now >= nextReconnectAt) {
                attempt++;
                if (!reconnectBackoff.allowsAttempt(attempt)) {
                    LOG.error("Could not re-establish the listen connection for channel '{}' within {} attempts "
                            + "- continuing as a plain poll every {} ms", channel, reconnectBackoff.maxAttempts(),
                            pollTimeoutMillis);
                    reconnecting = false;
                } else if (reconnect()) {
                    attempt = 0;
                    // Notifications sent while nothing was listening are gone, so catch up right away.
                    safeRun(onWakeup);
                    nextPollAt = System.currentTimeMillis() + pollTimeoutMillis;
                    continue;
                } else {
                    nextReconnectAt = System.currentTimeMillis() + reconnectBackoff.delay(attempt).toMillis();
                }
            }
            if (System.currentTimeMillis() >= nextPollAt) {
                safeRun(onWakeup);
                nextPollAt = System.currentTimeMillis() + pollTimeoutMillis;
            }
            final long wakeAt = reconnecting ? Math.min(nextPollAt, nextReconnectAt) : nextPollAt;
            if (!sleepQuietly(clamp(wakeAt - System.currentTimeMillis(), 1, MAX_SLICE_MILLIS))) {
                return;
            }
        }
    }

    private boolean reconnect() {
        Connection conn = null;
        try {
            conn = requireFactory().create();
            listen(conn);
            connection = conn;
            LOG.info("Re-established the listen connection for channel '{}'", channel);
            return true;
        } catch (final SQLException ex) {
            if (conn != null) {
                closeQuietly(conn);
            }
            LOG.debug("Could not re-establish the listen connection for channel '{}': {}", channel, ex.toString());
            return false;
        }
    }

    private PgListenConnectionFactory requireFactory() {
        final PgListenConnectionFactory factory = connectionFactory;
        if (factory == null) {
            throw new IllegalStateException("No connection factory was configured");
        }
        return factory;
    }

    private void listen(final Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("LISTEN \"" + channel + "\"");
        }
    }

    private void closeQuietly(final Connection conn) {
        if (!ownsConnection) {
            return;
        }
        try {
            conn.close();
        } catch (final SQLException ex) {
            LOG.debug("Error closing the listen connection for channel '{}': {}", channel, ex.toString());
        }
    }

    private static long clamp(final long value, final long min, final long max) {
        return Math.max(min, Math.min(value, max));
    }

    private void safeRun(final Runnable onWakeup) {
        try {
            onWakeup.run();
        } catch (final RuntimeException ex) {
            LOG.error("Wake-up callback failed for channel '{}'", channel, ex);
        }
    }

    private boolean sleepQuietly(final long millis) {
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
        final Connection conn = connection;
        if (conn == null) {
            return;
        }
        connection = null;
        try (Statement st = conn.createStatement()) {
            st.execute("UNLISTEN \"" + channel + "\"");
        } catch (final SQLException ex) {
            LOG.warn("Failed to UNLISTEN channel '{}' on close", channel, ex);
        }
        closeQuietly(conn);
    }

}
