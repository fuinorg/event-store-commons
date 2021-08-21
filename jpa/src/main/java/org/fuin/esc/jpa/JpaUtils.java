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

import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.Nullable;

/**
 * Package utilities.
 */
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
        if (streamId.getParameters().size() == 0) {
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
        if (streamId instanceof JpaStreamId) {
            final JpaStreamId jpaId = (JpaStreamId) streamId;
            return jpaId.getNativeTableName();
        }
        // Default ID
        if (streamId.isProjection()) {
            return camel2Underscore(streamId.getName());
        }
        if (streamId.getParameters().size() == 0) {
            return NoParamsEvent.NO_PARAMS_EVENTS_TABLE;
        }
        return camel2Underscore(streamId.getName()) + "_events";
    }
    
    /**
     * Converts the given camel case name into a name with underscores.
     * 
     * @param name Name to convert.
     * 
     * @return Camel case replaced with underscores.
     */
    public static String camel2Underscore(@Nullable final String name) {
        if (name == null) {
            return null;
        }
        return name.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
    }

    
    
}
