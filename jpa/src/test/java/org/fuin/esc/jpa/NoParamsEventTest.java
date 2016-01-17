/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.junit.Test;

// CHECKSTYLE:OFF
public final class NoParamsEventTest {

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(NoParamsEvent.class)
                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE).verify();
    }

    @Test
    public void testGetter() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("Abc");
        final Integer eventNumber = 3;
        final JpaEvent eventEntry = new JpaEvent();
        final NoParamsEvent testee = new NoParamsEvent(streamId, eventNumber, eventEntry);

        // TEST
        assertThat(testee.getStreamName()).isEqualTo(streamId.getName());
        assertThat(testee.getEventNumber()).isEqualTo(eventNumber);
        assertThat(testee.getEvent()).isEqualTo(eventEntry);

    }

}
// CHECKSTYLE:ON
