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

import org.fuin.objects4j.common.ThreadSafe;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Creates the dedicated JDBC connection a {@link PgListenNotifyWakeupSource} runs {@code LISTEN} on, so the
 * source can replace it after the connection dropped.
 * <p>
 * This is deliberately not a {@code DataSource}: a listening connection blocks on notifications and is held
 * for the lifetime of the source, so taking it from the application's pool would remove one connection from
 * that pool indefinitely. Hand out a connection opened for this purpose - for example
 * {@code () -> DriverManager.getConnection(url, user, password)}.
 */
@FunctionalInterface
@ThreadSafe
public interface PgListenConnectionFactory {

    /**
     * Opens a new connection dedicated to listening.
     *
     * @return Fresh connection, owned by the caller of this method.
     * @throws SQLException The connection could not be opened.
     */
    Connection create() throws SQLException;

}
