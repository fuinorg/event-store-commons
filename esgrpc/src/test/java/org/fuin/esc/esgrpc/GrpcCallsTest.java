package org.fuin.esc.esgrpc;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link GrpcCalls}.
 */
class GrpcCallsTest {

    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(200);

    @Test
    void testAwaitReturnsResult() throws Exception {
        final CompletableFuture<String> future = CompletableFuture.completedFuture("abc");

        assertThat(GrpcCalls.await(future, SHORT_TIMEOUT, "op")).isEqualTo("abc");
    }

    @Test
    void testAwaitNeverCompletedFutureFailsWithTimeout() {
        // This is the situation that used to hang forever: the KurrentDB client queued the work item
        // but nothing ever completed the future.
        final CompletableFuture<String> neverCompleted = new CompletableFuture<>();

        assertThatThrownBy(() -> GrpcCalls.await(neverCompleted, SHORT_TIMEOUT, "appendToStream"))
                .isInstanceOf(EventStoreCallTimeoutException.class)
                .hasMessageContaining("appendToStream")
                .hasMessageContaining("200")
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);
    }

    @Test
    void testAwaitCancelsTheFutureOnTimeout() {
        final CompletableFuture<String> neverCompleted = new CompletableFuture<>();

        assertThatThrownBy(() -> GrpcCalls.await(neverCompleted, SHORT_TIMEOUT, "op"))
                .isInstanceOf(EventStoreCallTimeoutException.class);

        assertThat(neverCompleted).isCancelled();
    }

    @Test
    void testAwaitPropagatesExecutionException() {
        // A failure answered by the server must stay an ExecutionException, because the callers map
        // its cause to the matching event store exception.
        final IllegalStateException cause = new IllegalStateException("boom");
        final CompletableFuture<String> failed = CompletableFuture.failedFuture(cause);

        assertThatThrownBy(() -> GrpcCalls.await(failed, SHORT_TIMEOUT, "op"))
                .isInstanceOf(ExecutionException.class)
                .hasCause(cause);
    }

    @Test
    void testAwaitTimeoutIsNotAnExecutionException() {
        // Callers like streamExists(..) map an ExecutionException to a result value ("false"), so a
        // timeout must never arrive as one - otherwise "no answer" silently becomes a wrong answer.
        final CompletableFuture<String> neverCompleted = new CompletableFuture<>();

        assertThatThrownBy(() -> GrpcCalls.await(neverCompleted, SHORT_TIMEOUT, "streamExists"))
                .isNotInstanceOf(ExecutionException.class);
    }

    @Test
    void testDefaultCallTimeout() {
        assertThat(GrpcCalls.DEFAULT_CALL_TIMEOUT).isEqualTo(Duration.ofSeconds(5));
    }

}
