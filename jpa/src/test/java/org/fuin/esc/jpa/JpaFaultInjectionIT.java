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
import org.fuin.esc.api.EscConnectionException;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.utils4j.TestOmitted;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Fault-injection test for the JPA event store against a <b>real</b> PostgreSQL: it takes the database away
 * and stalls it, and checks that the store reports {@link EscConnectionException} rather than a raw
 * {@link jakarta.persistence.PersistenceException}, and that it does so within the configured bound instead
 * of waiting forever.
 * <p>
 * The unit tests pin the same mapping with a proxy {@code EntityManager}; what only a real database can show
 * is that the driver and the dialect actually produce the failures the mapping keys on - a
 * {@code PersistenceException} carrying a JDBC connectivity cause, and a lock acquisition that honours
 * {@code jakarta.persistence.lock.timeout}.
 * <p>
 * Each test gets its own container because one of them destroys the database. Self-skips when no container
 * engine is reachable.
 */
@TestOmitted("Integration test - it has no separate '*Test' counterpart")
@Testcontainers(disabledWithoutDocker = true)
class JpaFaultInjectionIT extends AbstractTest {

    /** Short on purpose: the assertion is that a blocked lock gives up rather than waiting for the holder. */
    private static final JpaTimeouts SHORT_TIMEOUTS =
            new JpaTimeouts(Duration.ofSeconds(2), Duration.ofSeconds(2));

    private static final StreamId STREAM = new SimpleStreamId("S-FAULT");

    @Test
    void aStalledLockGivesUpInsteadOfWaitingForTheHolder() {

        try (PostgreSQLContainer<?> db = postgres()) {
            db.start();
            final EntityManagerFactory emf = emf(db, "create-drop");
            try {
                // PREPARE: the stream row has to be committed first - an uncommitted INSERT is invisible to
                // the other transaction, which would then simply create its own row instead of contending.
                final EntityManager holder = emf.createEntityManager();
                final EntityManager blocked = emf.createEntityManager();
                try {
                    holder.getTransaction().begin();
                    eventStore(holder).appendToStream(STREAM, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event("one"));
                    holder.getTransaction().commit();

                    // Now take and keep the PESSIMISTIC_WRITE lock on that committed row.
                    holder.getTransaction().begin();
                    eventStore(holder).appendToStream(STREAM, ExpectedVersion.ANY.getNo(), event("two"));

                    // TEST: transaction 2 wants the same row. Without a lock timeout it waits for as long as
                    // the holder keeps the lock, which is unbounded.
                    blocked.getTransaction().begin();
                    final long start = System.currentTimeMillis();
                    assertTimeoutPreemptively(Duration.ofSeconds(60), () ->
                            assertThatThrownBy(() -> eventStore(blocked)
                                    .appendToStream(STREAM, ExpectedVersion.ANY.getNo(), event("three")))
                                    .isInstanceOf(EscConnectionException.class));
                    final long elapsed = System.currentTimeMillis() - start;

                    // VERIFY: it really contended for the lock and then gave up when the timeout elapsed.
                    // Failing much faster would mean the two transactions never met on the same row, which
                    // is how an earlier version of this test passed without proving anything.
                    assertThat(elapsed).isGreaterThanOrEqualTo(SHORT_TIMEOUTS.lockTimeout().toMillis());
                } finally {
                    rollbackQuietly(blocked);
                    rollbackQuietly(holder);
                    blocked.close();
                    holder.close();
                }
            } finally {
                emf.close();
            }
        }
    }

    @Test
    void aDatabaseThatWentAwayIsReportedAsATransientFailure() {

        try (PostgreSQLContainer<?> db = postgres()) {
            db.start();
            // "create" rather than "create-drop": the database is gone by the time the factory closes, so a
            // drop at shutdown would fail the test in its teardown instead of in its assertion.
            final EntityManagerFactory emf = emf(db, "create");
            try {
                final EntityManager em = emf.createEntityManager();
                try {
                    // PREPARE: prove the wiring works before breaking it
                    final JpaEventStore es = eventStore(em);
                    em.getTransaction().begin();
                    es.appendToStream(STREAM, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event("one"));
                    em.getTransaction().commit();
                    assertThat(es.streamExists(STREAM)).isTrue();

                    // TEST: the database goes away under the open connection
                    db.stop();

                    // VERIFY: a consumer classifies this with one instanceof instead of digging through
                    // PersistenceException causes for an SQLState
                    assertTimeoutPreemptively(Duration.ofSeconds(60), () ->
                            assertThatThrownBy(() -> es.readEventsForward(STREAM, 0, 10))
                                    .isInstanceOf(EscConnectionException.class));
                } finally {
                    rollbackQuietly(em);
                    em.close();
                }
            } finally {
                closeQuietly(emf);
            }
        }
    }

    private JpaEventStore eventStore(final EntityManager em) {
        return JpaEventStore.builder()
                .em(em)
                .streamFactory(new JpaIdStreamFactory() {
                    @Override
                    public boolean containsType(final StreamId streamId) {
                        return true;
                    }

                    @Override
                    public JpaStream createStream(final StreamId streamId) {
                        return new NoParamsStream(streamId);
                    }
                })
                .serRegistry(getSerDeserializerRegistry())
                .desRegistry(getSerDeserializerRegistry())
                .timeouts(SHORT_TIMEOUTS)
                .build();
    }

    private static SimpleCommonEvent event(final String text) {
        return new SimpleCommonEvent(new EventId(), EventA.TYPE, new EventA(text), null);
    }

    private static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"));
    }

    private static EntityManagerFactory emf(final PostgreSQLContainer<?> db, final String hbm2ddl) {
        final Map<String, Object> props = new HashMap<>();
        props.put("hibernate.connection.url", db.getJdbcUrl());
        props.put("hibernate.connection.username", db.getUsername());
        props.put("hibernate.connection.password", db.getPassword());
        props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.hbm2ddl.auto", hbm2ddl);
        return Persistence.createEntityManagerFactory("testPU", props);
    }

    private static void closeQuietly(final EntityManagerFactory emf) {
        try {
            emf.close();
        } catch (final RuntimeException ex) { // NOSONAR - the database was taken away on purpose
            // Nothing left to do
        }
    }

    private static void rollbackQuietly(final EntityManager em) {
        try {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } catch (final RuntimeException ex) { // NOSONAR - the database may already be gone
            // Nothing left to do
        }
    }

}
