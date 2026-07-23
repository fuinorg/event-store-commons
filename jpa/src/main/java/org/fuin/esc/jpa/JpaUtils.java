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

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.Query;
import jakarta.persistence.QueryTimeoutException;
import org.fuin.esc.api.EscConnectionException;
import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.ThreadSafe;

import java.io.IOException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Package utilities.
 */
@ThreadSafe
final class JpaUtils {

    private JpaUtils() {
        // Never used
    }

    /**
     * Bounds how long the given query may run.
     *
     * @param <T>   Type of the query.
     * @param query Query to bound.
     * @return The same query, for chaining.
     */
    static <T extends Query> T withQueryTimeout(final JpaTimeouts timeouts, final T query) {
        query.setHint("jakarta.persistence.query.timeout", timeouts.queryTimeoutMillis());
        return query;
    }

    /**
     * Bounds how long the given query waits for a pessimistic lock.
     *
     * @param <T>   Type of the query.
     * @param query Query to bound.
     * @return The same query, for chaining.
     */
    static <T extends Query> T withLockTimeout(final JpaTimeouts timeouts, final T query) {
        query.setHint("jakarta.persistence.lock.timeout", timeouts.lockTimeoutMillis());
        return query;
    }

    /**
     * Executes a database operation and translates a transient failure into an
     * {@link EscConnectionException}. Applied where the query is actually executed, so every caller gets the
     * typed exception without repeating the mapping.
     *
     * @param <T>       Type of the result.
     * @param operation Operation to execute.
     * @return Result of the operation.
     */
    static <T> T execute(final Supplier<T> operation) {
        try {
            return operation.get();
        } catch (final RuntimeException ex) {
            throw mapPersistenceException(ex);
        }
    }

    /**
     * Executes a database operation without a result and translates a transient failure into an
     * {@link EscConnectionException}.
     *
     * @param operation Operation to execute.
     */
    static void execute(final Runnable operation) {
        try {
            operation.run();
        } catch (final RuntimeException ex) {
            throw mapPersistenceException(ex);
        }
    }

    /**
     * Translates a database failure into an {@link EscConnectionException} if it is transient - the database
     * could not be reached, the query or the lock ran into its timeout, or a lock could not be obtained.
     * Anything else is returned unchanged, especially {@link jakarta.persistence.NoResultException} (the
     * callers map it to a business exception) and {@link jakarta.persistence.OptimisticLockException} (a
     * concurrency conflict, which is a business answer and must not be retried blindly).
     *
     * @param ex Failure to inspect.
     * @return Either an {@link EscConnectionException} or the original failure.
     */
    static RuntimeException mapPersistenceException(final RuntimeException ex) {
        if (ex instanceof QueryTimeoutException || ex instanceof LockTimeoutException
                || ex instanceof PessimisticLockException) {
            return new EscConnectionException("The database did not answer in time: " + ex.getMessage(), ex);
        }
        if (ex instanceof PersistenceException && hasConnectivityCause(ex)) {
            return new EscConnectionException("Could not reach the database: " + ex.getMessage(), ex);
        }
        return ex;
    }

    /**
     * Walks the cause chain looking for a JDBC or network level connectivity failure.
     *
     * @param error Failure to inspect.
     * @return {@literal true} if the database was not reachable.
     */
    private static boolean hasConnectivityCause(final Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLTransientException || t instanceof SQLRecoverableException
                    || t instanceof SQLNonTransientConnectionException || t instanceof IOException) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /**
     * Returns the name of the stream entity for a given stream.
     *
     * @param streamId
     *            Identifier of the stream to return a stream entity name for.
     *
     * @return Name of the entity (simple class name).
     */
    public static String streamEntityName(final StreamId streamId) {
        // User defined ID
        if (streamId instanceof JpaStreamId) {
            final JpaStreamId jpaId = (JpaStreamId) streamId;
            return jpaId.getEntityName();
        }
        // Default ID
        if (streamId.isProjection()) {
            return streamId.getName();
        }
        if (streamId.getParameters().isEmpty()) {
            return NoParamsStream.class.getSimpleName();
        }
        return streamId.getName() + "Stream";
    }

    /**
     * Returns a native database events table name.
     *
     * @param streamId Unique stream identifier.
     *
     * @return Name that is configured in the {@link jakarta.persistence.Table} JPA annotation.
     */
    public static String nativeEventsTableName(final StreamId streamId) {
        // User defined ID
        if (streamId instanceof JpaStreamId jpaId) {
            return jpaId.getNativeTableName();
        }
        // Default ID
        if (streamId.isProjection()) {
            return Objects.requireNonNull(camel2Underscore(streamId.getName()));
        }
        if (streamId.getParameters().isEmpty()) {
            return NoParamsEvent.NO_PARAMS_EVENTS_TABLE;
        }
        return camel2Underscore(streamId.getName()) + "_events";
    }

    /**
     * Converts the given camel case name into a lower-case name with underscores. An underscore is inserted
     * only at a lower-case/digit to upper-case boundary, so a run of upper-case letters (an acronym or an
     * all-upper-case name) is treated as a single word: {@code "PersonName"} becomes {@code "person_name"},
     * {@code "personId"} becomes {@code "person_id"} and {@code "PERSON"} becomes {@code "person"}.
     *
     * @param name Name to convert.
     *
     * @return Camel case replaced with underscores.
     */
    @Nullable
    public static String camel2Underscore(@Nullable final String name) {
        if (name == null) {
            return null;
        }
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }


}
