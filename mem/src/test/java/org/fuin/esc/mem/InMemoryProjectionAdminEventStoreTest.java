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

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.ProjectionAdminEventStore;
import org.fuin.esc.api.ProjectionAlreadyExistsException;
import org.fuin.esc.api.ProjectionId;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.TypeName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link InMemoryProjectionAdminEventStore} together with the projection read path of
 * {@link InMemoryEventStore}.
 */
public class InMemoryProjectionAdminEventStoreTest {

    private static final EventId E1 = new EventId("11111111-1111-1111-1111-111111111111");
    private static final EventId E2 = new EventId("22222222-2222-2222-2222-222222222222");
    private static final EventId E3 = new EventId("33333333-3333-3333-3333-333333333333");
    private static final EventId E4 = new EventId("44444444-4444-4444-4444-444444444444");

    private InMemoryEventStore es;

    private ProjectionAdminEventStore admin;

    @BeforeEach
    public void setup() {
        es = new InMemoryEventStore(Executors.newCachedThreadPool());
        es.open();
        admin = es.getProjectionAdmin().open();

        // Global append order: E1, E2, E3, E4 (across three streams).
        es.appendToStream(new SimpleStreamId("s1"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                event(E1, "TypeA", List.of("Created")));
        es.appendToStream(new SimpleStreamId("s2"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                event(E2, "TypeB", List.of("Archived")));
        es.appendToStream(new SimpleStreamId("s1"), 0,
                event(E3, "TypeA", List.of("Created", "Archived")));
        es.appendToStream(new SimpleStreamId("s3"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                event(E4, "TypeC", List.of()));
    }

    @AfterEach
    public void teardown() {
        admin.close();
        es.close();
        es = null;
        admin = null;
    }

    @Test
    public void testCapabilities() {
        assertThat(es.capabilities().projections()).isTrue();
    }

    @Test
    public void testSelectByType() {
        admin.createProjection(new ProjectionId("byType"), new ProjectionStreamId("byType"), true,
                List.of(new TypeName("TypeA")), List.of());

        assertThat(readIds("byType")).containsExactly(E1, E3);
    }

    @Test
    public void testSelectByCategory() {
        admin.createProjection(new ProjectionId("byCat"), new ProjectionStreamId("byCat"), true,
                List.of(), List.of("Created"));

        assertThat(readIds("byCat")).containsExactly(E1, E3);
    }

    @Test
    public void testSelectByTypeOrCategory() {
        // TypeB OR category 'Archived' -> E2 (TypeB + Archived) and E3 (Archived), in global order.
        admin.createProjection(new ProjectionId("mixed"), new ProjectionStreamId("mixed"), true,
                List.of(new TypeName("TypeB")), List.of("Archived"));

        assertThat(readIds("mixed")).containsExactly(E2, E3);
    }

    @Test
    public void testPaging() {
        admin.createProjection(new ProjectionId("byCat"), new ProjectionStreamId("byCat"), true,
                List.of(), List.of("Created"));

        final StreamEventsSlice first = es.readEventsForward(new ProjectionStreamId("byCat"), 0, 1);
        assertThat(first.getEvents()).extracting(CommonEvent::getId).containsExactly(E1);
        assertThat(first.getFromEventNumber()).isEqualTo(0);
        assertThat(first.getNextEventNumber()).isEqualTo(1);
        assertThat(first.isEndOfStream()).isFalse();

        final StreamEventsSlice second = es.readEventsForward(new ProjectionStreamId("byCat"), 1, 10);
        assertThat(second.getEvents()).extracting(CommonEvent::getId).containsExactly(E3);
        assertThat(second.getFromEventNumber()).isEqualTo(1);
        assertThat(second.getNextEventNumber()).isEqualTo(2);
        assertThat(second.isEndOfStream()).isTrue();
    }

    @Test
    public void testDisabledReturnsEmpty() {
        admin.createProjection(new ProjectionId("disabled"), new ProjectionStreamId("disabled"), false,
                List.of(), List.of("Created"));

        final StreamEventsSlice slice = es.readEventsForward(new ProjectionStreamId("disabled"), 0, 10);
        assertThat(slice.getEvents()).isEmpty();
        assertThat(slice.isEndOfStream()).isTrue();

        // Enabling makes it return the events.
        admin.enableProjection(new ProjectionId("disabled"));
        assertThat(readIds("disabled")).containsExactly(E1, E3);

        // Disabling again makes it empty.
        admin.disableProjection(new ProjectionId("disabled"));
        assertThat(es.readEventsForward(new ProjectionStreamId("disabled"), 0, 10).getEvents()).isEmpty();
    }

    @Test
    public void testExistsAndDelete() {
        assertThat(admin.projectionExists(new ProjectionId("p"))).isFalse();
        admin.createProjection(new ProjectionId("p"), new ProjectionStreamId("p"), true,
                List.of(new TypeName("TypeA")), List.of());
        assertThat(admin.projectionExists(new ProjectionId("p"))).isTrue();

        admin.deleteProjection(new ProjectionId("p"));
        assertThat(admin.projectionExists(new ProjectionId("p"))).isFalse();
        assertThatThrownBy(() -> es.readEventsForward(new ProjectionStreamId("p"), 0, 10))
                .isInstanceOf(StreamNotFoundException.class);
    }

    @Test
    public void testCreateDuplicateFails() {
        admin.createProjection(new ProjectionId("dup"), new ProjectionStreamId("dup"), true,
                List.of(new TypeName("TypeA")), List.of());
        assertThatThrownBy(() -> admin.createProjection(new ProjectionId("dup"), new ProjectionStreamId("dup"), true,
                List.of(new TypeName("TypeA")), List.of()))
                .isInstanceOf(ProjectionAlreadyExistsException.class);
    }

    @Test
    public void testUnknownProjectionStreamNotFound() {
        assertThatThrownBy(() -> es.readEventsForward(new ProjectionStreamId("unknown"), 0, 10))
                .isInstanceOf(StreamNotFoundException.class);
    }

    @Test
    public void testEnableDeleteUnknownThrows() {
        assertThatThrownBy(() -> admin.enableProjection(new ProjectionId("nope")))
                .isInstanceOf(StreamNotFoundException.class);
        assertThatThrownBy(() -> admin.deleteProjection(new ProjectionId("nope")))
                .isInstanceOf(StreamNotFoundException.class);
    }

    private List<EventId> readIds(final String projectionName) {
        final StreamEventsSlice slice = es.readEventsForward(new ProjectionStreamId(projectionName), 0, 100);
        return slice.getEvents().stream().map(CommonEvent::getId).toList();
    }

    private static CommonEvent event(final EventId id, final String type, final List<String> categories) {
        return new SimpleCommonEvent(id, new TypeName(type), new MyEvent(id.asBaseType().toString()),
                null, null, null, categories);
    }

}
