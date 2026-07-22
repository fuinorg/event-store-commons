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

import org.fuin.objects4j.common.NotThreadSafe;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Signals that the event store could not be reached: a connection problem, a call that did not complete in
 * time, or the store answering that it is currently unavailable.
 * <p>
 * <b>The contract is "transient": the operation may be retried.</b> This is deliberately distinct from the
 * business exceptions of this API ({@link WrongExpectedVersionException}, {@link StreamNotFoundException},
 * {@link StreamDeletedException}, {@link StreamAlreadyExistsException}, {@link StreamReadOnlyException},
 * {@link ProjectionAlreadyExistsException} and {@link EventNotFoundException}), which are answers from the
 * store and must <em>never</em> be retried - retrying them cannot succeed.
 * <p>
 * Retry and circuit breaker predicates should therefore be able to decide with a single
 * {@code instanceof EscConnectionException}.
 * <p>
 * <b>Retrying is only safe subject to idempotency.</b> When a call fails this way the outcome is unknown:
 * a write may or may not have been applied, because the failure can happen after the store accepted it.
 * Reads can always be repeated; appends need either an expected version or a deduplication mechanism.
 */
@NotThreadSafe
public class EscConnectionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1000L;

    /**
     * Constructor with message.
     *
     * @param message Description of the problem.
     */
    public EscConnectionException(final String message) {
        super(message);
    }

    /**
     * Constructor with message and cause.
     *
     * @param message Description of the problem.
     * @param cause   Original failure (may be {@literal null}).
     */
    public EscConnectionException(final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }

}
