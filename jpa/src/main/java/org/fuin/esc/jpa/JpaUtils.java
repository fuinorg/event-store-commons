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

import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Package utilities.
 */
@ThreadSafe
final class JpaUtils {

    private JpaUtils() {
        // Never used
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
