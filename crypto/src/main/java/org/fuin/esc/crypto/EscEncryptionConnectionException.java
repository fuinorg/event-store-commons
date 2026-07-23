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
package org.fuin.esc.crypto;

import org.fuin.esc.api.EscConnectionException;
import org.fuin.objects4j.common.NotThreadSafe;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Signals that the external key service backing the {@link EncryptingEventStore} could not be reached or did
 * not answer in time - the vault is down, the network is broken, or the call ran into its timeout.
 * <p>
 * It extends {@link EscConnectionException} on purpose: a store whose key service is unavailable is
 * unavailable, and a consumer should be able to classify it with the same single
 * {@code instanceof EscConnectionException} that covers the event store and the database. The dedicated type
 * is only there for a consumer that wants to tell "the vault is down" from "the store is down", for example
 * to fall back to serving events that need no decryption.
 * <p>
 * <b>The contract is "transient": the operation may be retried</b>, subject to the same idempotency rules as
 * any other transient failure - see {@link org.fuin.esc.api.WritableEventStore} for what that means for an
 * append. It is deliberately distinct from {@link EscEncryptionException}, which reports a definite answer
 * from the key service (the key or key version is unknown, the data could not be decrypted) and must never
 * be retried.
 */
@NotThreadSafe
public final class EscEncryptionConnectionException extends EscConnectionException {

    @Serial
    private static final long serialVersionUID = 1000L;

    /**
     * Constructor with message and cause.
     *
     * @param message Description of the problem.
     * @param cause   Original failure (may be {@literal null}).
     */
    public EscEncryptionConnectionException(final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }

}
