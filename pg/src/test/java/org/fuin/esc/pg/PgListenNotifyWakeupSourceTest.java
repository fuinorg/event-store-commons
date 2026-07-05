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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for {@link PgListenNotifyWakeupSource} against a real PostgreSQL in a Docker container.
 * Proves that an {@code INSERT} (via the {@link PgNotify} trigger) wakes the callback with "read now" latency
 * well below the poll interval, that the poll safety-net fires without any notification, and the start/close
 * lifecycle. Skipped automatically when no Docker environment is available.
 */
@Testcontainers(disabledWithoutDocker = true)
class PgListenNotifyWakeupSourceTest {

    private static final String CHANNEL = "esc_events";

    private static final String TABLE = "demo_events";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    private Connection listenConn;

    private Connection writeConn;

    @BeforeEach
    void setup() throws SQLException {
        listenConn = newConnection();
        writeConn = newConnection();
        try (Statement st = writeConn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (id serial primary key)");
        }
        try (Statement st = writeConn.createStatement()) {
            st.execute(PgNotify.installTriggerSql(TABLE, CHANNEL));
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (listenConn != null) {
            listenConn.close();
        }
        if (writeConn != null) {
            writeConn.close();
        }
    }

    @Test
    void firesQuicklyOnInsert() throws Exception {
        final BlockingQueue<Object> wakeups = new LinkedBlockingQueue<>();
        // Large poll interval: any wake within a few seconds of the insert must be notify-driven, not polled.
        try (PgListenNotifyWakeupSource source = new PgListenNotifyWakeupSource(listenConn, CHANNEL, 30, TimeUnit.SECONDS)) {
            source.start(() -> wakeups.add(Boolean.TRUE));

            // Initial catch-up fires immediately.
            assertThat(wakeups.poll(3, TimeUnit.SECONDS)).as("initial catch-up wake").isNotNull();
            // Without an insert nothing else arrives (poll interval is 30s).
            assertThat(wakeups.poll(500, TimeUnit.MILLISECONDS)).as("no wake without an insert").isNull();

            insertRow();

            assertThat(wakeups.poll(5, TimeUnit.SECONDS)).as("notify-driven wake after insert").isNotNull();
        }
    }

    @Test
    void firesOnPollIntervalWithoutNotify() throws Exception {
        final BlockingQueue<Object> wakeups = new LinkedBlockingQueue<>();
        try (PgListenNotifyWakeupSource source = new PgListenNotifyWakeupSource(listenConn, CHANNEL, 200, TimeUnit.MILLISECONDS)) {
            source.start(() -> wakeups.add(Boolean.TRUE));

            // Initial catch-up, then the safety-net keeps firing on the interval with no inserts at all.
            assertThat(wakeups.poll(2, TimeUnit.SECONDS)).as("initial wake").isNotNull();
            assertThat(wakeups.poll(2, TimeUnit.SECONDS)).as("safety-net wake 1").isNotNull();
            assertThat(wakeups.poll(2, TimeUnit.SECONDS)).as("safety-net wake 2").isNotNull();
        }
    }

    @Test
    void rejectsSecondStartAndClosesCleanly() throws Exception {
        final PgListenNotifyWakeupSource source = new PgListenNotifyWakeupSource(listenConn, CHANNEL, 1, TimeUnit.SECONDS);
        source.start(() -> {
        });

        assertThatThrownBy(() -> source.start(() -> {
        })).isInstanceOf(IllegalStateException.class);

        source.close();
        source.close(); // idempotent

        assertThat(listenConn.isClosed()).as("caller-owned connection stays open after close()").isFalse();
    }

    private void insertRow() throws SQLException {
        try (Statement st = writeConn.createStatement()) {
            st.execute("INSERT INTO " + TABLE + " DEFAULT VALUES");
        }
    }

    private static Connection newConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

}
