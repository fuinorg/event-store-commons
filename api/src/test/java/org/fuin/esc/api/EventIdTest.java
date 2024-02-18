package org.fuin.esc.api;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Test for the {@link EventId} class.
 */
public class EventIdTest {

    @Test
    void testEqualsHashCode() {
        EqualsVerifier.forClass(EventId.class).verify();
    }

}
