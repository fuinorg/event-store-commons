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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.fuin.esc.api.EscConnectionException;
import org.fuin.esc.api.ProjectionId;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TypeName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.SQLTransientConnectionException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the entity manager access of {@link JpaCheckpointStore} and
 * {@link JpaProjectionAdminEventStore} reports an unreachable database as an {@link EscConnectionException}
 * instead of a raw {@link PersistenceException}, so a consumer can classify it with a single
 * {@code instanceof} like every other database access in this module.
 */
public final class JpaStoreConnectionFailureTest {

    private static final StreamId STREAM = new SimpleStreamId("MyView");

    private static final ProjectionId PROJECTION = new ProjectionId("MyProjection");

    /**
     * Returns an entity manager whose every method fails the way a broken connection does.
     *
     * @return Entity manager that cannot reach the database.
     */
    private static EntityManager brokenEm() {
        return (EntityManager) Proxy.newProxyInstance(
                JpaStoreConnectionFailureTest.class.getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    throw new PersistenceException("Unable to acquire JDBC connection",
                            new SQLTransientConnectionException("Connection is closed"));
                });
    }

    @Test
    public void testReadCheckpoint() {
        assertThatThrownBy(() -> new JpaCheckpointStore(brokenEm()).readCheckpoint(STREAM))
                .isInstanceOf(EscConnectionException.class);
    }

    @Test
    public void testUpdateCheckpoint() {
        assertThatThrownBy(() -> new JpaCheckpointStore(brokenEm()).updateCheckpoint(STREAM, 3))
                .isInstanceOf(EscConnectionException.class);
    }

    @Test
    public void testResetCheckpoint() {
        assertThatThrownBy(() -> new JpaCheckpointStore(brokenEm()).resetCheckpoint(STREAM))
                .isInstanceOf(EscConnectionException.class);
    }

    @Test
    public void testProjectionExists() {
        assertThatThrownBy(() -> new JpaProjectionAdminEventStore(brokenEm()).projectionExists(PROJECTION))
                .isInstanceOf(EscConnectionException.class);
    }

    @Test
    public void testCreateProjection() {
        assertThatThrownBy(() -> new JpaProjectionAdminEventStore(brokenEm()).createProjection(
                PROJECTION, new ProjectionStreamId("MyProjection"), true, List.of(new TypeName("EventA")), List.of()))
                .isInstanceOf(EscConnectionException.class);
    }

    @Test
    public void testDeleteProjection() {
        assertThatThrownBy(() -> new JpaProjectionAdminEventStore(brokenEm()).deleteProjection(PROJECTION))
                .isInstanceOf(EscConnectionException.class);
    }

}
