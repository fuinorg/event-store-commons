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
import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.objects4j.crypto.DecryptionFailedException;
import org.fuin.objects4j.crypto.EncryptionKeyIdUnknownException;
import org.fuin.objects4j.crypto.EncryptionKeyVersionUnknownException;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs a call against the external key service so that an outage of that service is reported as a typed
 * transient failure instead of whatever the client implementation happens to throw, and - when a timeout is
 * configured - cannot block the calling thread forever.
 */
@ThreadSafe
final class KeyServiceCalls {

    private KeyServiceCalls() {
        throw new UnsupportedOperationException("It is not allowed to create an instance of this utility class");
    }

    /**
     * A call to the key service. The three checked exceptions are definite answers from the service and pass
     * through untouched; anything else is a candidate for the transient classification.
     *
     * @param <T> Type of the result.
     */
    @FunctionalInterface
    interface Call<T> {

        /**
         * Performs the call.
         *
         * @return Result of the call.
         * @throws EncryptionKeyIdUnknownException      The given key identifier is unknown.
         * @throws EncryptionKeyVersionUnknownException The given version of the key is unknown.
         * @throws DecryptionFailedException            The data could not be decrypted.
         */
        T call() throws EncryptionKeyIdUnknownException, EncryptionKeyVersionUnknownException,
                DecryptionFailedException;
    }

    /**
     * Executes a key service call, optionally bounded by a timeout.
     *
     * @param <T>       Type of the result.
     * @param call      Call to perform.
     * @param operation Name of the operation, used in the error message.
     * @param timeout   Maximum time the call may take, or {@literal null} to wait as long as the client does.
     * @param executor  Executor used to run the call when a timeout is configured.
     * @return Result of the call.
     * @throws EncryptionKeyIdUnknownException      The given key identifier is unknown.
     * @throws EncryptionKeyVersionUnknownException The given version of the key is unknown.
     * @throws DecryptionFailedException            The data could not be decrypted.
     * @throws EscEncryptionConnectionException     The key service could not be reached or did not answer in
     *                                              time.
     */
    static <T> T execute(final Call<T> call, final String operation, @Nullable final Duration timeout,
                         @Nullable final ExecutorService executor)
            throws EncryptionKeyIdUnknownException, EncryptionKeyVersionUnknownException,
            DecryptionFailedException {

        if (timeout == null || executor == null) {
            try {
                return call.call();
            } catch (final RuntimeException ex) {
                throw mapIfTransient(ex, operation);
            }
        }

        final CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return call.call();
            } catch (final EncryptionKeyIdUnknownException | EncryptionKeyVersionUnknownException
                           | DecryptionFailedException ex) {
                throw new CompletionException(ex);
            }
        }, executor);

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final TimeoutException ex) {
            // Nothing is going to consume the result anymore. The call itself cannot be aborted, so the
            // worker keeps running until the client's own socket timeout fires - what is bounded here is the
            // wait of the thread that appends or reads, which is the one that must not hang.
            future.cancel(true);
            throw new EscEncryptionConnectionException("The key service call '" + operation
                    + "' did not complete within " + timeout.toMillis() + " ms", ex);
        } catch (final InterruptedException ex) { // NOSONAR
            Thread.currentThread().interrupt();
            throw new EscEncryptionConnectionException("Interrupted waiting for the key service call '"
                    + operation + "'", ex);
        } catch (final ExecutionException ex) {
            throw rethrow(ex.getCause(), operation);
        }
    }

    /**
     * Rethrows the cause of a failed call with its original type.
     *
     * @param cause     Failure to rethrow.
     * @param operation Name of the operation.
     * @return Never returns - the return type only lets the caller write {@code throw rethrow(..)}.
     * @throws EncryptionKeyIdUnknownException      The given key identifier is unknown.
     * @throws EncryptionKeyVersionUnknownException The given version of the key is unknown.
     * @throws DecryptionFailedException            The data could not be decrypted.
     */
    private static RuntimeException rethrow(@Nullable final Throwable cause, final String operation)
            throws EncryptionKeyIdUnknownException, EncryptionKeyVersionUnknownException,
            DecryptionFailedException {
        if (cause instanceof EncryptionKeyIdUnknownException ex) {
            throw ex;
        }
        if (cause instanceof EncryptionKeyVersionUnknownException ex) {
            throw ex;
        }
        if (cause instanceof DecryptionFailedException ex) {
            throw ex;
        }
        if (cause instanceof RuntimeException ex) {
            return mapIfTransient(ex, operation);
        }
        return new EscEncryptionConnectionException("The key service call '" + operation + "' failed", cause);
    }

    /**
     * Translates a key service failure into an {@link EscEncryptionConnectionException} if it looks
     * transient, and returns it unchanged otherwise.
     * <p>
     * Classification walks the cause chain for an {@link IOException} or a {@link TimeoutException}, which is
     * what an HTTP client reports when the vault is unreachable, refused the connection or timed out. A
     * client that can classify better - because it understands the service's own status codes - should throw
     * an {@link EscConnectionException} itself; those are passed through untouched.
     *
     * @param ex        Failure to inspect.
     * @param operation Name of the operation.
     * @return Either an {@link EscEncryptionConnectionException} or the original failure.
     */
    static RuntimeException mapIfTransient(final RuntimeException ex, final String operation) {
        if (ex instanceof EscConnectionException) {
            // Already classified - do not hide the more specific message.
            return ex;
        }
        if (hasConnectivityCause(ex)) {
            return new EscEncryptionConnectionException("Could not reach the key service executing '"
                    + operation + "'", ex);
        }
        return ex;
    }

    /**
     * Walks the cause chain looking for a network level failure.
     *
     * @param error Failure to inspect.
     * @return {@literal true} if the key service was not reachable.
     */
    private static boolean hasConnectivityCause(final Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof IOException || t instanceof TimeoutException) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

}
