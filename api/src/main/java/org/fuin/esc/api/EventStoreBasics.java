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

import org.fuin.objects4j.common.ThreadSafe;

/**
 * Basic synchronous operations shared by all event store types.
 * <p>
 * Implementations are expected to be thread-safe.
 */
@ThreadSafe
public interface EventStoreBasics extends AutoCloseable {

    /**
     * Opens a connection to the event store. Opening an already opened event
     * store is ignored.
     *
     * @return This event store.
     */
    EventStoreBasics open();

    /**
     * Closes the connection to the event store. Closing an already closed or
     * never opened event store is simply ignored.
     */
    void close();

    /**
     * Returns the optional capabilities this backend provides. Consumers can use the descriptor to detect at
     * wiring time whether the store supports the features they need (subscriptions, projections, ...) instead
     * of discovering gaps by runtime failure. The default is {@link EventStoreCapabilities#NONE}; concrete
     * backends override it to report their true capabilities.
     *
     * @return Backend capabilities (never {@literal null}).
     */
    default EventStoreCapabilities capabilities() {
        return EventStoreCapabilities.NONE;
    }

}
