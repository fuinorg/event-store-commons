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

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Serves an event at a version an external consumer understands by reusing the directed converter chains of a
 * {@link ConverterRegistry}. Where {@link ConverterRegistry#upcastToLatest(SerializedDataType, String, Object)}
 * always lifts <em>up</em> to the current representation, this helper resolves a <em>specific</em> target version
 * (up-cast, down-cast or skip) - the down-cast direction being what content negotiation with older clients
 * requires during a rolling deployment.
 * <p>
 * No conversion logic of its own: it merely picks a reachable target version and delegates the actual work to the
 * registry's {@link ConverterRegistry#convert(SerializedDataType, String, String, Object) convert} method. Only
 * the directions for which converters are registered are reachable (a {@code v2 → v1} down-cast needs a
 * {@code v2 → v1} converter, exactly as up-casting needs {@code v1 → v2}).
 * <p>
 * As the wrapped registry is immutable and thread safe, so is this negotiator.
 */
@ThreadSafe
public final class VersionNegotiator {

    private final ConverterRegistry converters;

    /**
     * Constructor with the registry the negotiator delegates conversions to.
     *
     * @param converters Registry holding the version chains.
     */
    public VersionNegotiator(final ConverterRegistry converters) {
        super();
        Contract.requireArgNotNull("converters", converters);
        this.converters = converters;
    }

    /**
     * Converts the given object to a single explicit target version. Returns the input unchanged when
     * {@code fromVersion} equals {@code toVersion}.
     *
     * @param type        Logical type of the data.
     * @param fromVersion Version the {@code data} object currently represents (may be {@literal null}).
     * @param toVersion   Target version to convert to.
     * @param data        Object to convert.
     * @param <T>         Type the object is converted into.
     * @return Object converted to {@code toVersion}.
     * @throws IllegalArgumentException If no chain of registered converters leads from {@code fromVersion} to
     *                                  {@code toVersion}.
     */
    public <T> T to(final SerializedDataType type, @Nullable final String fromVersion, final String toVersion,
                    final Object data) {
        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("toVersion", toVersion);
        Contract.requireArgNotNull("data", data);
        return converters.convert(type, fromVersion, toVersion, data);
    }

    /**
     * Serves the given object at the first of the {@code acceptedVersions} that is reachable from
     * {@code currentVersion} via the registered chains. The accepted versions are tried in the order given, so
     * the caller should list them by decreasing preference. A version equal to {@code currentVersion} is served
     * as-is; any other is attempted through {@link ConverterRegistry#convert convert}, skipping the ones with no
     * chain.
     *
     * @param type             Logical type of the data.
     * @param currentVersion   Version the {@code data} object currently represents (may be {@literal null}).
     * @param acceptedVersions Versions the consumer understands, in decreasing order of preference.
     * @param data             Object to serve.
     * @param <T>              Type the object is converted into.
     * @return Object at the first reachable accepted version, or {@link Optional#empty()} if none is reachable.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> negotiate(final SerializedDataType type, @Nullable final String currentVersion,
                                     final List<String> acceptedVersions, final Object data) {
        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("acceptedVersions", acceptedVersions);
        Contract.requireArgNotNull("data", data);
        for (final String accepted : acceptedVersions) {
            if (accepted.equals(currentVersion)) {
                return Optional.of((T) data);
            }
            try {
                return Optional.of(converters.convert(type, currentVersion, accepted, data));
            } catch (final IllegalArgumentException ex) {
                // No chain to this accepted version - try the next one.
            }
        }
        return Optional.empty();
    }

}
