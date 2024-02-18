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
package org.fuin.esc.jpa;

import jakarta.persistence.*;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public final class NoParamsEventTest {

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(NoParamsEvent.class)
                .withIgnoredAnnotations(Entity.class, Id.class, Embeddable.class, MappedSuperclass.class,
                        Transient.class)
                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE, Warning.ALL_FIELDS_SHOULD_BE_USED)
                .verify();
    }

    @Test
    public void testGetter() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("Abc");
        final Long eventNumber = 3L;
        final JpaEvent eventEntry = new JpaEvent();
        final NoParamsEvent testee = new NoParamsEvent(streamId, eventNumber, eventEntry);

        // TEST
        assertThat(testee.getStreamName()).isEqualTo(streamId.getName());
        assertThat(testee.getEventNumber()).isEqualTo(eventNumber);
        assertThat(testee.getEvent()).isEqualTo(eventEntry);

    }

}

