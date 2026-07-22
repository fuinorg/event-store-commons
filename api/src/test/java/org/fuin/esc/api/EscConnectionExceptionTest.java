package org.fuin.esc.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link EscConnectionException}.
 */
class EscConnectionExceptionTest {

    @Test
    void testCreateWithMessage() {
        final EscConnectionException testee = new EscConnectionException("Store not reachable");

        assertThat(testee.getMessage()).isEqualTo("Store not reachable");
        assertThat(testee.getCause()).isNull();
    }

    @Test
    void testCreateWithCause() {
        final RuntimeException cause = new RuntimeException("boom");

        final EscConnectionException testee = new EscConnectionException("Store not reachable", cause);

        assertThat(testee.getMessage()).isEqualTo("Store not reachable");
        assertThat(testee.getCause()).isSameAs(cause);
    }

    @Test
    void testIsNotABusinessException() {
        // The whole point of the type: a retry predicate decides with a single instanceof, and the
        // business exceptions of this API must never match it.
        final EscConnectionException testee = new EscConnectionException("Store not reachable");

        assertThat(testee).isInstanceOf(RuntimeException.class);
        assertThat(testee).isNotInstanceOf(StreamNotFoundException.class);
        assertThat(testee).isNotInstanceOf(StreamDeletedException.class);
        assertThat(testee).isNotInstanceOf(WrongExpectedVersionException.class);
    }

}
