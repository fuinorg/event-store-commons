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
package org.fuin.esc.mem;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.Utils4J.deserialize;
import static org.fuin.utils4j.Utils4J.serialize;

/**
 * Tests the class {@link InMemorySubscription}.
 */

public class InMemorySubscriptionTest {

    @Test
    public void testSerDeserialize() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("MyStream");
        final long lastEventNumber = 1;
        final int subscriberId = 4711;
        final InMemorySubscription original = new InMemorySubscription(
                subscriberId, streamId, lastEventNumber);

        // TEST
        final InMemorySubscription copy = deserialize(serialize(original));

        // VERIFY
        assertThat(copy).isEqualTo(original);

    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(InMemorySubscription.class).suppress(Warning.ALL_FIELDS_SHOULD_BE_USED).verify();
    }

}

