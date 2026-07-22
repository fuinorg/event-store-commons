package org.fuin.esc.jpa;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link JpaTimeouts}.
 */
class JpaTimeoutsTest {

    @Test
    void testDefault() {
        assertThat(JpaTimeouts.DEFAULT.queryTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(JpaTimeouts.DEFAULT.lockTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void testMillisConversion() {
        final JpaTimeouts testee = new JpaTimeouts(Duration.ofSeconds(3), Duration.ofMillis(1500));

        assertThat(testee.queryTimeoutMillis()).isEqualTo(3000);
        assertThat(testee.lockTimeoutMillis()).isEqualTo(1500);
    }

    @Test
    void testRejectsNonPositive() {
        // A zero or negative timeout would silently mean "no limit" in some providers, which is exactly
        // the unbounded wait this class exists to prevent.
        assertThatThrownBy(() -> new JpaTimeouts(Duration.ZERO, Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JpaTimeouts(Duration.ofSeconds(5), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsNull() {
        assertThatThrownBy(() -> new JpaTimeouts(null, Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JpaTimeouts(Duration.ofSeconds(5), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
