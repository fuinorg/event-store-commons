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

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.ProjectionId;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that a projection stream is served by {@link JpaEventStore} as a type filter over the global event
 * log. Because a projection selects events by type across the whole database (which is shared for the test
 * class), the two test methods use disjoint event types ({@code EventA} vs {@code EventB}) so they cannot
 * interfere with each other.
 */
public final class JpaProjectionEventStoreTest extends AbstractPersistenceTest {

    private JpaEventStore createEventStore() {
        return new JpaEventStore(getEm(), new JpaIdStreamFactory() {
            @Override
            public JpaStream createStream(final StreamId streamId) {
                return new NoParamsStream(streamId);
            }

            @Override
            public boolean containsType(final StreamId streamId) {
                return true;
            }
        }, getSerDeserializerRegistry(), getSerDeserializerRegistry());
    }

    @Test
    public void testProjectionFiltersByTypeInGlobalOrderWithPaging() {

        // PREPARE - append 3 events across 3 streams, interleaving the selected (EventA) and other (EventB)
        // types, then create an enabled projection selecting only EventA.
        final ProjectionId projectionId = new ProjectionId("EventAProjection");
        final ProjectionStreamId projectionStreamId = new ProjectionStreamId("EventAProjection");
        final EventId idA1 = new EventId();
        final EventId idB1 = new EventId();
        final EventId idA2 = new EventId();

        beginTransaction();
        try (final JpaEventStore es = createEventStore()) {
            es.open();
            es.appendToStream(new SimpleStreamId("StreamA1"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                    new SimpleCommonEvent(idA1, EventA.TYPE, new EventA("a1"), null));
            es.appendToStream(new SimpleStreamId("StreamB1"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                    new SimpleCommonEvent(idB1, EventB.TYPE, new EventB("b1"), null));
            es.appendToStream(new SimpleStreamId("StreamA2"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                    new SimpleCommonEvent(idA2, EventA.TYPE, new EventA("a2"), null));
            new JpaProjectionAdminEventStore(getEm()).createProjection(projectionId, projectionStreamId, true,
                    List.of(EventA.TYPE));
        }
        commitTransaction();

        // TEST & VERIFY - the projection returns only the two EventA events, in global (append) order.
        beginTransaction();
        try (final JpaEventStore es = createEventStore()) {
            es.open();

            assertThat(es.streamExists(projectionStreamId)).isTrue();

            final StreamEventsSlice all = es.readEventsForward(projectionStreamId, 0, 10);
            assertThat(all.getEvents()).extracting(CommonEvent::getId).containsExactly(idA1, idA2);
            assertThat(all.getNextEventNumber()).isEqualTo(2);
            assertThat(all.isEndOfStream()).isTrue();

            // Paging: one at a time, using the returned next event number as the offset.
            final StreamEventsSlice page0 = es.readEventsForward(projectionStreamId, 0, 1);
            assertThat(page0.getEvents()).extracting(CommonEvent::getId).containsExactly(idA1);
            assertThat(page0.getNextEventNumber()).isEqualTo(1);
            assertThat(page0.isEndOfStream()).isFalse();

            final StreamEventsSlice page1 = es.readEventsForward(projectionStreamId, page0.getNextEventNumber(), 1);
            assertThat(page1.getEvents()).extracting(CommonEvent::getId).containsExactly(idA2);
            assertThat(page1.getNextEventNumber()).isEqualTo(2);

            final StreamEventsSlice page2 = es.readEventsForward(projectionStreamId, page1.getNextEventNumber(), 1);
            assertThat(page2.getEvents()).isEmpty();
            assertThat(page2.isEndOfStream()).isTrue();
        }
        commitTransaction();
    }

    @Test
    public void testDisabledProjectionReturnsEmpty() {

        // PREPARE - append an EventB and create a DISABLED projection selecting EventB.
        final ProjectionId projectionId = new ProjectionId("EventBProjection");
        final ProjectionStreamId projectionStreamId = new ProjectionStreamId("EventBProjection");

        beginTransaction();
        try (final JpaEventStore es = createEventStore()) {
            es.open();
            es.appendToStream(new SimpleStreamId("StreamBx"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                    new SimpleCommonEvent(new EventId(), EventB.TYPE, new EventB("bx"), null));
            new JpaProjectionAdminEventStore(getEm()).createProjection(projectionId, projectionStreamId, false,
                    List.of(EventB.TYPE));
        }
        commitTransaction();

        // TEST & VERIFY - a not-yet-enabled projection is reported as not existing and reads empty.
        beginTransaction();
        try (final JpaEventStore es = createEventStore()) {
            es.open();
            assertThat(es.streamExists(projectionStreamId)).isFalse();
            final StreamEventsSlice slice = es.readEventsForward(projectionStreamId, 0, 10);
            assertThat(slice.getEvents()).isEmpty();
            assertThat(slice.isEndOfStream()).isTrue();
        }
        commitTransaction();
    }

}
