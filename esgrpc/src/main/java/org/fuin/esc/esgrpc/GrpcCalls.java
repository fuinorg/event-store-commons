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

import org.fuin.objects4j.common.ThreadSafe;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Helper for waiting on the futures returned by the KurrentDB client.
 * <p>
 * The client hands the call to an internal worker and completes the future from a callback thread.
 * If that thread never runs - the connection was never established, the worker died, or the work
 * item is queued behind a broken connection - the future is never completed. Waiting on it without a
 * timeout therefore blocks the calling thread forever instead of failing, which turns an
 * infrastructure problem into a hanging application (or a CI job that runs until it is killed).
 */
@ThreadSafe
final class GrpcCalls {

    /**
     * Default time to wait for a single event store call. A healthy store answers in milliseconds, so a
     * call that is still pending after this long is treated as a connectivity problem rather than as slow
     * progress. Raise it via the builder if a deployment genuinely needs longer (very large batches, a
     * slow link).
     */
    static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(5);

    private GrpcCalls() {
        throw new UnsupportedOperationException("It is not allowed to create an instance of this utility class");
    }

    /**
     * Waits for the result of an event store call, but never longer than the given timeout.
     *
     * @param <T>       Type of the result.
     * @param future    Future to wait for.
     * @param timeout   Maximum time to wait.
     * @param operation Name of the operation, used in the error message.
     * @return Result of the call.
     * @throws ExecutionException            The call itself failed - handled by the caller exactly as before.
     * @throws InterruptedException          The waiting thread was interrupted.
     * @throws EventStoreCallTimeoutException The call did not complete within the timeout.
     */
    static <T> T await(final CompletableFuture<T> future, final Duration timeout, final String operation)
            throws ExecutionException, InterruptedException {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final TimeoutException ex) {
            // Nothing is going to consume the result anymore.
            future.cancel(true);
            throw new EventStoreCallTimeoutException(operation, timeout, ex);
        }
    }

    /**
     * Bounds an asynchronous event store call. The returned stage fails with an
     * {@link EventStoreCallTimeoutException} if the call did not complete within the given timeout, which
     * makes the asynchronous API fail exactly like the synchronous one instead of handing the caller a
     * future that is never completed. The client's own future is only copied, not modified, so the client
     * remains free to complete it later; it is cancelled on a timeout because nobody consumes it anymore.
     *
     * @param <T>       Type of the result.
     * @param future    Future returned by the client.
     * @param timeout   Maximum time to wait.
     * @param operation Name of the operation, used in the error message.
     * @return Future that is guaranteed to complete within the timeout.
     */
    static <T> CompletableFuture<T> within(final CompletableFuture<T> future, final Duration timeout,
                                           final String operation) {
        final CompletableFuture<T> bounded = future.copy();
        bounded.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        return bounded.exceptionallyCompose(ex -> {
            if (rootCause(ex) instanceof TimeoutException cause) {
                // Nothing is going to consume the result anymore.
                future.cancel(true);
                return CompletableFuture.failedFuture(new EventStoreCallTimeoutException(operation, timeout, cause));
            }
            return CompletableFuture.failedFuture(ex);
        });
    }

    /**
     * Unwraps the cause from the wrapper exceptions added by the {@link CompletableFuture} chain.
     *
     * @param throwable Throwable to unwrap.
     * @return Root cause to translate.
     */
    static Throwable rootCause(final Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

}
