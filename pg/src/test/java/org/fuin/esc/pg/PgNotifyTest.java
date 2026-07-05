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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Docker-less unit tests for the {@link PgNotify} DDL helper and the argument validation of
 * {@link PgListenNotifyWakeupSource} (the runtime behaviour is covered by
 * {@code PgListenNotifyWakeupSourceTest} against a real PostgreSQL).
 */
public class PgNotifyTest {

    @Test
    public void testInstallTriggerSql() {

        // TEST
        final String ddl = PgNotify.installTriggerSql("demo_events", "esc_events");

        // VERIFY
        assertThat(ddl)
                .contains("pg_notify('esc_events', '')")
                .contains("AFTER INSERT ON \"demo_events\"")
                .contains("FOR EACH STATEMENT")
                .contains("\"demo_events_esc_notify\"")
                .contains("\"demo_events_esc_notify_trg\"");
    }

    @Test
    public void testInstallTriggerSqlRejectsBadTable() {
        assertThatThrownBy(() -> PgNotify.installTriggerSql("bad-table", "esc_events"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testInstallTriggerSqlRejectsBadChannel() {
        assertThatThrownBy(() -> PgNotify.installTriggerSql("demo_events", "bad channel"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConstructorRejectsBadChannel() {
        assertThatThrownBy(() -> new PgListenNotifyWakeupSource(noOpConnection(), "bad-channel", 1, TimeUnit.SECONDS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Returns a no-op {@link Connection} proxy. The constructor only stores the connection (it is used first in
     * {@code start(..)}), so the proxy is never actually invoked here.
     */
    private static Connection noOpConnection() {
        return (Connection) Proxy.newProxyInstance(PgNotifyTest.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> null);
    }

}
