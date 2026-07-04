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
package org.fuin.esc.api;

import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

/**
 * Locates and composes {@link Converter}s that lift a deserialized event from the version it was stored with
 * to a newer in-memory representation. Converters are registered per {@link SerializedDataType} as a directed
 * chain keyed by the source version (the {@code version} parameter of the stored
 * {@link EnhancedMimeType}). Applying the chain {@code v1 → v2 → … → vN} hands consumers a single <em>current</em>
 * representation instead of forcing them to branch on version.
 * <p>
 * The rule for schema evolution is: <em>a new "version" of an event must be convertible from the old one; if it
 * is not, it is a new event type, not a new version.</em>
 * <p>
 * All implementations are expected to be thread safe.
 */
@ThreadSafe
public interface ConverterRegistry {

    /**
     * Lifts the given object to the latest version by following the registered chain from {@code fromVersion}
     * until no further converter is registered. Returns the input unchanged when {@code fromVersion} is
     * {@literal null} or already the latest version (no outgoing converter).
     *
     * @param type        Logical type of the data.
     * @param fromVersion Version the {@code data} object currently represents (may be {@literal null} for an
     *                    unversioned event).
     * @param data        Object to lift.
     * @param <T>         Type the object is converted into.
     * @return Object converted to the latest version (may be the same instance).
     */
    <T> T upcastToLatest(SerializedDataType type, @Nullable String fromVersion, Object data);

    /**
     * Converts the given object from one version to another by following the registered chain. Supports
     * forward (up-cast), reverse (down-cast) and skip conversions - whatever directed converters are
     * registered. Returns the input unchanged when {@code fromVersion} equals {@code toVersion}.
     *
     * @param type        Logical type of the data.
     * @param fromVersion Version the {@code data} object currently represents (may be {@literal null}).
     * @param toVersion   Target version to convert to.
     * @param data        Object to convert.
     * @param <T>         Type the object is converted into.
     * @return Converted object.
     * @throws IllegalArgumentException If no chain of registered converters leads from {@code fromVersion} to
     *                                  {@code toVersion}.
     */
    <T> T convert(SerializedDataType type, @Nullable String fromVersion, String toVersion, Object data);

    /**
     * Returns whether a converter with the given source version is registered for the type.
     *
     * @param type        Logical type of the data.
     * @param fromVersion Source version (may be {@literal null}).
     * @return TRUE if an outgoing converter exists.
     */
    boolean converterExists(SerializedDataType type, @Nullable String fromVersion);

    /**
     * Defines a builder for the registry.
     *
     * @param <T> Type of the registry.
     * @param <B> Type of the builder.
     */
    interface Builder<T extends ConverterRegistry, B extends Builder<T, B>> {

        /**
         * Adds a converter that lifts an event of the given type from one version to the next.
         *
         * @param type        Logical type of the data.
         * @param fromVersion Source version the converter accepts.
         * @param toVersion   Target version the converter produces.
         * @param converter   Converter performing the conversion.
         * @return This builder.
         */
        B add(SerializedDataType type, String fromVersion, String toVersion, Converter<?, ?> converter);

        /**
         * Builds an instance of the registry.
         *
         * @return New instance.
         */
        T build();

    }

}
