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
package org.fuin.esc.jpa;

import org.fuin.objects4j.common.Immutable;

import java.time.Duration;

/**
 * How long database operations of the JPA event store may take.
 * <p>
 * Both values bound a wait that is otherwise unbounded: a query runs as long as the database lets it, and a
 * pessimistic lock waits as long as the holder keeps it. Both turn a struggling database into a blocked
 * application thread, which is why they default to a short value rather than to "no limit".
 * <p>
 * This does not replace the connection pool's own {@code connection-timeout}: obtaining a connection happens
 * before any query runs and is owned by the application's datasource. Size it so that the worst case
 * (pool wait + query timeout) is still acceptable for the caller.
 *
 * @param queryTimeout Maximum time a single query may run.
 * @param lockTimeout  Maximum time to wait for a pessimistic lock.
 */
@Immutable
public record JpaTimeouts(Duration queryTimeout, Duration lockTimeout) {

    /** Default used when nothing else is configured. */
    public static final JpaTimeouts DEFAULT = new JpaTimeouts(Duration.ofSeconds(5), Duration.ofSeconds(5));

    /**
     * Constructor with all data.
     *
     * @param queryTimeout Maximum time a single query may run.
     * @param lockTimeout  Maximum time to wait for a pessimistic lock.
     */
    public JpaTimeouts {
        if (queryTimeout == null) {
            throw new IllegalArgumentException("queryTimeout cannot be null");
        }
        if (lockTimeout == null) {
            throw new IllegalArgumentException("lockTimeout cannot be null");
        }
        if (queryTimeout.isNegative() || queryTimeout.isZero()) {
            throw new IllegalArgumentException("queryTimeout must be positive, but was: " + queryTimeout);
        }
        if (lockTimeout.isNegative() || lockTimeout.isZero()) {
            throw new IllegalArgumentException("lockTimeout must be positive, but was: " + lockTimeout);
        }
    }

    /**
     * Returns the query timeout in milliseconds, the unit the JPA hint expects.
     *
     * @return Query timeout in milliseconds.
     */
    public int queryTimeoutMillis() {
        return (int) queryTimeout.toMillis();
    }

    /**
     * Returns the lock timeout in milliseconds, the unit the JPA hint expects.
     *
     * @return Lock timeout in milliseconds.
     */
    public int lockTimeoutMillis() {
        return (int) lockTimeout.toMillis();
    }

}
