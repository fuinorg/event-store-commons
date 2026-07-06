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

import org.fuin.esc.api.ProjectionAlreadyExistsException;
import org.fuin.esc.api.ProjectionId;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.TypeName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for the {@link JpaProjectionAdminEventStore} class. Every test method uses a distinct projection name
 * so the projections it creates do not interfere with the other methods (the schema is shared for the class).
 */
public final class JpaProjectionAdminEventStoreTest extends AbstractPersistenceTest {

    @Test
    public void testCreateAndExists() {

        // PREPARE
        final JpaProjectionAdminEventStore testee = new JpaProjectionAdminEventStore(getEm());
        final ProjectionId projectionId = new ProjectionId("CreateProjection");
        final ProjectionStreamId streamId = new ProjectionStreamId("CreateProjection");
        assertThat(testee.projectionExists(projectionId)).isFalse();

        // TEST
        beginTransaction();
        testee.createProjection(projectionId, streamId, true, List.of(new TypeName("EventA"), new TypeName("EventB")));
        commitTransaction();

        // VERIFY
        assertThat(testee.projectionExists(projectionId)).isTrue();
        final JpaProjection persisted = getEm().find(JpaProjection.class, "CreateProjection");
        assertThat(persisted.isEnabled()).isTrue();
        assertThat(persisted.getEventTypes()).containsExactlyInAnyOrder("EventA", "EventB");
    }

    @Test
    public void testCreateDuplicateFails() {

        // PREPARE
        final JpaProjectionAdminEventStore testee = new JpaProjectionAdminEventStore(getEm());
        final ProjectionId projectionId = new ProjectionId("DuplicateProjection");
        final ProjectionStreamId streamId = new ProjectionStreamId("DuplicateProjection");
        beginTransaction();
        testee.createProjection(projectionId, streamId, true, List.of(new TypeName("EventA")));
        commitTransaction();

        // TEST & VERIFY
        beginTransaction();
        try {
            assertThatThrownBy(() -> testee.createProjection(projectionId, streamId, true, List.of(new TypeName("EventA"))))
                    .isInstanceOf(ProjectionAlreadyExistsException.class);
        } finally {
            rollbackTransaction();
        }
    }

    @Test
    public void testEnableDisable() {

        // PREPARE
        final JpaProjectionAdminEventStore testee = new JpaProjectionAdminEventStore(getEm());
        final ProjectionId projectionId = new ProjectionId("ToggleProjection");
        final ProjectionStreamId streamId = new ProjectionStreamId("ToggleProjection");
        beginTransaction();
        testee.createProjection(projectionId, streamId, false, List.of(new TypeName("EventA")));
        commitTransaction();
        assertThat(getEm().find(JpaProjection.class, "ToggleProjection").isEnabled()).isFalse();

        // TEST enable
        beginTransaction();
        testee.enableProjection(projectionId);
        commitTransaction();
        assertThat(getEm().find(JpaProjection.class, "ToggleProjection").isEnabled()).isTrue();

        // TEST disable
        beginTransaction();
        testee.disableProjection(projectionId);
        commitTransaction();
        assertThat(getEm().find(JpaProjection.class, "ToggleProjection").isEnabled()).isFalse();
    }

    @Test
    public void testDelete() {

        // PREPARE
        final JpaProjectionAdminEventStore testee = new JpaProjectionAdminEventStore(getEm());
        final ProjectionId projectionId = new ProjectionId("DeletableProjection");
        final ProjectionStreamId streamId = new ProjectionStreamId("DeletableProjection");
        beginTransaction();
        testee.createProjection(projectionId, streamId, true, List.of(new TypeName("EventA")));
        commitTransaction();

        // TEST
        beginTransaction();
        testee.deleteProjection(projectionId);
        commitTransaction();

        // VERIFY
        assertThat(testee.projectionExists(projectionId)).isFalse();
    }

    @Test
    public void testEnableMissingFails() {
        final JpaProjectionAdminEventStore testee = new JpaProjectionAdminEventStore(getEm());
        assertThatThrownBy(() -> testee.enableProjection(new ProjectionId("GhostProjection")))
                .isInstanceOf(StreamNotFoundException.class);
    }

}
