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
package org.fuin.esc.esgrpc;

import org.fuin.esc.api.EscConnectionException;
import org.fuin.objects4j.common.NotThreadSafe;

import java.io.Serial;
import java.time.Duration;

/**
 * A call to the event store did not complete within the configured timeout.
 * <p>
 * This is deliberately an unchecked exception that is <em>not</em> an
 * {@link java.util.concurrent.ExecutionException}: the calling methods interpret an execution
 * exception as an answer from the server (for example
 * {@code streamExists(..)} maps a {@code StatusRuntimeException} to {@code false}), so reporting a
 * timeout that way would turn "no answer at all" into a wrong result. A timeout means the outcome of
 * the operation is unknown - for a write it may or may not have been applied.
 */
@NotThreadSafe
public final class EventStoreCallTimeoutException extends EscConnectionException {

    @Serial
    private static final long serialVersionUID = 1000L;

    private final String operation;

    private final Duration timeout;

    /**
     * Constructor with all data.
     *
     * @param operation Name of the operation that timed out (like "appendToStream").
     * @param timeout   Timeout that elapsed.
     * @param cause     Original timeout exception.
     */
    public EventStoreCallTimeoutException(final String operation, final Duration timeout,
                                          final Throwable cause) {
        super("The event store call '" + operation + "' did not complete within " + timeout.toMillis()
                + " ms - the outcome of the operation is unknown", cause);
        this.operation = operation;
        this.timeout = timeout;
    }

    /**
     * Returns the name of the operation that timed out.
     *
     * @return Operation name.
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Returns the timeout that elapsed.
     *
     * @return Timeout.
     */
    public Duration getTimeout() {
        return timeout;
    }

}
