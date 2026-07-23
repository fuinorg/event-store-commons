package org.fuin.esc.esgrpc;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
    void testWithinReturnsResult() {
        final CompletableFuture<String> future = CompletableFuture.completedFuture("abc");

        assertThat(GrpcCalls.within(future, SHORT_TIMEOUT, "op")).isCompletedWithValue("abc");
    }

    @Test
    void testWithinNeverCompletedFutureFailsWithTimeout() {
        // The asynchronous counterpart of the hang: the caller used to get a future that is never
        // completed, so a "wait for the result" further up the chain blocked forever.
        final CompletableFuture<String> neverCompleted = new CompletableFuture<>();

        assertThatThrownBy(() -> GrpcCalls.within(neverCompleted, SHORT_TIMEOUT, "appendToStream").join())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(EventStoreCallTimeoutException.class)
                .hasMessageContaining("appendToStream")
                .hasMessageContaining("200")
                .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void testWithinCancelsTheFutureOnTimeout() {
        final CompletableFuture<String> neverCompleted = new CompletableFuture<>();

        assertThatThrownBy(() -> GrpcCalls.within(neverCompleted, SHORT_TIMEOUT, "op").join())
                .isInstanceOf(CompletionException.class);

        assertThat(neverCompleted).isCancelled();
    }

    @Test
    void testWithinPropagatesTheOriginalFailure() {
        // A failure answered by the server must reach the caller unchanged, because the callers map it
        // to the matching event store exception.
        final IllegalStateException cause = new IllegalStateException("boom");
        final CompletableFuture<String> failed = CompletableFuture.failedFuture(cause);

        assertThatThrownBy(() -> GrpcCalls.within(failed, SHORT_TIMEOUT, "op").join())
                .isInstanceOf(CompletionException.class)
                .hasRootCause(cause);
    }

    @Test
    void testWithinDoesNotCompleteTheClientFuture() {
        // Only a copy is bounded, so the client remains free to complete its own future later.
        final CompletableFuture<String> slow = new CompletableFuture<>();
        final CompletableFuture<String> bounded = GrpcCalls.within(slow, Duration.ofHours(1), "op");

        slow.complete("abc");

        assertThat(bounded).isCompletedWithValue("abc");
    }

    @Test
    void testDefaultCallTimeout() {
        assertThat(GrpcCalls.DEFAULT_CALL_TIMEOUT).isEqualTo(Duration.ofSeconds(5));
    }

}
