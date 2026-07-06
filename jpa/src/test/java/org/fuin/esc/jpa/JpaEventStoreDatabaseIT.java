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
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.ProjectionId;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.utils4j.TestOmitted;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that runs the JPA event store against two <b>real</b> relational databases via
 * Testcontainers - MariaDB and PostgreSQL - to catch backend-specific behaviour the in-memory HSQLDB unit
 * tests cannot: native sequence id generation, {@code @Lob} mapping, identifier case handling, and the
 * projection's native "select by type, ordered by global id, paged" query. Self-skips when no container
 * engine is reachable.
 */
@TestOmitted("Integration test - it has no separate '*Test' counterpart")
@Testcontainers(disabledWithoutDocker = true)
class JpaEventStoreDatabaseIT extends AbstractTest {

    @Container
    private static final MariaDBContainer<?> MARIADB =
            new MariaDBContainer<>(DockerImageName.parse("mariadb:12.3"));

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"));

    @Test
    void mariaDb() {
        runScenario(MARIADB, "org.mariadb.jdbc.Driver", "org.hibernate.dialect.MariaDBDialect");
    }

    @Test
    void postgreSql() {
        runScenario(POSTGRES, "org.postgresql.Driver", "org.hibernate.dialect.PostgreSQLDialect");
    }

    /**
     * Drives the full store round trip (append, read, projection, checkpoint) against a single database.
     *
     * @param db      Running database container.
     * @param driver  JDBC driver class name.
     * @param dialect Hibernate dialect class name.
     */
    private void runScenario(final JdbcDatabaseContainer<?> db, final String driver, final String dialect) {

        final EntityManagerFactory emf = Persistence.createEntityManagerFactory("testPU", overrides(db, driver, dialect));
        try {
            final EntityManager em = emf.createEntityManager();

            final JpaEventStore es = new JpaEventStore(em, new JpaIdStreamFactory() {
                @Override
                public boolean containsType(final StreamId streamId) {
                    return true;
                }

                @Override
                public JpaStream createStream(final StreamId streamId) {
                    return new NoParamsStream(streamId);
                }
            }, getSerDeserializerRegistry(), getSerDeserializerRegistry());
            es.open();
            final JpaProjectionAdminEventStore admin = new JpaProjectionAdminEventStore(em);

            final EventId idA1 = new EventId();
            final EventId idB1 = new EventId();
            final EventId idA2 = new EventId();
            final ProjectionId projectionId = new ProjectionId("EventAProjection");
            final ProjectionStreamId projectionStreamId = new ProjectionStreamId("EventAProjection");

            // Append three events across three streams, interleaving the selected (EventA) and other (EventB)
            // types, and create an enabled projection selecting only EventA - all in one transaction.
            inTransaction(em, () -> {
                es.appendToStream(new SimpleStreamId("S-A1"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                        new SimpleCommonEvent(idA1, EventA.TYPE, new EventA("a1"), null));
                es.appendToStream(new SimpleStreamId("S-B1"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                        new SimpleCommonEvent(idB1, EventB.TYPE, new EventB("b1"), null));
                es.appendToStream(new SimpleStreamId("S-A2"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                        new SimpleCommonEvent(idA2, EventA.TYPE, new EventA("a2"), null));
                admin.createProjection(projectionId, projectionStreamId, true, List.of(EventA.TYPE));
            });

            // 1) Basic append/read of a single (NoParams) stream.
            assertThat(es.streamExists(new SimpleStreamId("S-A1"))).isTrue();
            final StreamEventsSlice streamSlice = es.readEventsForward(new SimpleStreamId("S-A1"), 0, 10);
            assertThat(streamSlice.getEvents()).extracting(CommonEvent::getId).containsExactly(idA1);

            // 2) Projection = type filter over the global event log: only the two EventA events, in global
            //    (append) order, with correct paging.
            assertThat(es.streamExists(projectionStreamId)).isTrue();
            final StreamEventsSlice all = es.readEventsForward(projectionStreamId, 0, 10);
            assertThat(all.getEvents()).extracting(CommonEvent::getId).containsExactly(idA1, idA2);
            assertThat(all.getNextEventNumber()).isEqualTo(2);
            assertThat(all.isEndOfStream()).isTrue();

            final StreamEventsSlice page0 = es.readEventsForward(projectionStreamId, 0, 1);
            assertThat(page0.getEvents()).extracting(CommonEvent::getId).containsExactly(idA1);
            assertThat(page0.getNextEventNumber()).isEqualTo(1);
            assertThat(page0.isEndOfStream()).isFalse();
            final StreamEventsSlice page1 = es.readEventsForward(projectionStreamId, page0.getNextEventNumber(), 1);
            assertThat(page1.getEvents()).extracting(CommonEvent::getId).containsExactly(idA2);

            // 3) Category projection: an event carrying a category is selected by category (not by type name),
            //    proving the queryable EVENT_CATEGORIES side table + the category filter work on the real DB.
            final EventId idCat = new EventId();
            final ProjectionId categoryProjectionId = new ProjectionId("DeletionProjection");
            final ProjectionStreamId categoryProjectionStreamId = new ProjectionStreamId("DeletionProjection");
            inTransaction(em, () -> {
                es.appendToStream(new SimpleStreamId("S-CAT"), ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(),
                        new SimpleCommonEvent(idCat, EventB.TYPE, new EventB("cat"), null, null, null,
                                List.of("Deletion")));
                new JpaProjectionAdminEventStore(em).createProjection(categoryProjectionId, categoryProjectionStreamId,
                        true, List.of(), List.of("Deletion"));
            });
            assertThat(es.streamExists(categoryProjectionStreamId)).isTrue();
            final StreamEventsSlice byCategory = es.readEventsForward(categoryProjectionStreamId, 0, 10);
            assertThat(byCategory.getEvents()).extracting(CommonEvent::getId).containsExactly(idCat);

            // 4) Catch-up checkpoint store round trip.
            final JpaCheckpointStore checkpoints = new JpaCheckpointStore(em);
            final StreamId checkpointStream = new SimpleStreamId("S-A1");
            inTransaction(em, () -> checkpoints.updateCheckpoint(checkpointStream, 7));
            assertThat(checkpoints.readCheckpoint(checkpointStream)).isEqualTo(7);
            inTransaction(em, () -> checkpoints.resetCheckpoint(checkpointStream));
            assertThat(checkpoints.readCheckpoint(checkpointStream)).isZero();

            em.close();
        } finally {
            emf.close();
        }
    }

    private static Map<String, Object> overrides(final JdbcDatabaseContainer<?> db, final String driver,
                                                 final String dialect) {
        final Map<String, Object> props = new HashMap<>();
        props.put("hibernate.connection.url", db.getJdbcUrl());
        props.put("hibernate.connection.username", db.getUsername());
        props.put("hibernate.connection.password", db.getPassword());
        props.put("hibernate.connection.driver_class", driver);
        props.put("hibernate.dialect", dialect);
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        return props;
    }

    private static void inTransaction(final EntityManager em, final Runnable action) {
        em.getTransaction().begin();
        try {
            action.run();
            em.getTransaction().commit();
        } catch (final RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        }
    }

}
