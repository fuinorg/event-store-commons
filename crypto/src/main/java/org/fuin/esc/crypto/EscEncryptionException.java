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

import org.fuin.objects4j.common.NotThreadSafe;

import java.io.Serial;

/**
 * Wraps the checked exceptions thrown by the {@link org.fuin.objects4j.crypto.EncryptedDataService}
 * into an unchecked exception so the {@link EncryptingEventStore} can comply with the
 * {@link org.fuin.esc.api.EventStore} method signatures.
 */
@NotThreadSafe
public final class EscEncryptionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor with message and cause.
     *
     * @param message Description of the failure.
     * @param cause   Original checked exception.
     */
    public EscEncryptionException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
