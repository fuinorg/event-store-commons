package org.fuin.esc.esgrpc;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link EventStoreCallTimeoutException}.
 */
class EventStoreCallTimeoutExceptionTest {

    @Test
    void testCreate() {
        final TimeoutException cause = new TimeoutException();

        final EventStoreCallTimeoutException testee =
                new EventStoreCallTimeoutException("appendToStream", Duration.ofSeconds(30), cause);

        assertThat(testee.getOperation()).isEqualTo("appendToStream");
        assertThat(testee.getTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(testee.getCause()).isSameAs(cause);
        assertThat(testee.getMessage()).contains("appendToStream").contains("30000");
    }

}
