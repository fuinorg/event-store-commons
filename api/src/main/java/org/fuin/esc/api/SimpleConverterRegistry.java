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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registry of {@link Converter}s composed into per-type version chains. Once built the registry is immutable
 * and therefore thread safe.
 */
@ThreadSafe
public final class SimpleConverterRegistry implements ConverterRegistry {

    private final Map<SerializedDataType, Map<String, Link>> chains;

    private SimpleConverterRegistry(final Map<SerializedDataType, Map<String, Link>> chains) {
        super();
        this.chains = chains;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T upcastToLatest(final SerializedDataType type, @Nullable final String fromVersion, final Object data) {
        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("data", data);
        if (fromVersion == null) {
            return (T) data;
        }
        final Map<String, Link> chain = chains.get(type);
        if (chain == null) {
            return (T) data;
        }
        final Set<String> visited = new HashSet<>();
        String current = fromVersion;
        Object result = data;
        Link link;
        while ((link = chain.get(current)) != null) {
            if (!visited.add(current)) {
                throw new IllegalStateException("Cyclic converter chain for type '" + type + "' at version '" + current + "'");
            }
            result = apply(link.converter, result);
            current = link.toVersion;
        }
        return (T) result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(final SerializedDataType type, @Nullable final String fromVersion, final String toVersion,
                         final Object data) {
        Contract.requireArgNotNull("type", type);
        Contract.requireArgNotNull("toVersion", toVersion);
        Contract.requireArgNotNull("data", data);
        if (toVersion.equals(fromVersion)) {
            return (T) data;
        }
        final Map<String, Link> chain = chains.get(type);
        if (chain == null || fromVersion == null) {
            throw noPath(type, fromVersion, toVersion);
        }
        final Set<String> visited = new HashSet<>();
        String current = fromVersion;
        Object result = data;
        Link link;
        while ((link = chain.get(current)) != null) {
            if (!visited.add(current)) {
                throw new IllegalStateException("Cyclic converter chain for type '" + type + "' at version '" + current + "'");
            }
            result = apply(link.converter, result);
            current = link.toVersion;
            if (current.equals(toVersion)) {
                return (T) result;
            }
        }
        throw noPath(type, fromVersion, toVersion);
    }

    @Override
    public boolean converterExists(final SerializedDataType type, @Nullable final String fromVersion) {
        Contract.requireArgNotNull("type", type);
        if (fromVersion == null) {
            return false;
        }
        final Map<String, Link> chain = chains.get(type);
        return chain != null && chain.containsKey(fromVersion);
    }

    @SuppressWarnings("unchecked")
    private static Object apply(final Converter<?, ?> converter, final Object data) {
        return ((Converter<Object, Object>) converter).convert(data);
    }

    private static IllegalArgumentException noPath(final SerializedDataType type, @Nullable final String fromVersion,
                                                   final String toVersion) {
        return new IllegalArgumentException(
                "No converter chain from version '" + fromVersion + "' to '" + toVersion + "' for type '" + type + "'");
    }

    /**
     * A single directed link in a version chain.
     */
    private record Link(String toVersion, Converter<?, ?> converter) {
    }

    /**
     * Builds instances of {@link SimpleConverterRegistry}. Single use: the internal state is reset after
     * {@link #build()}.
     */
    public static final class Builder implements ConverterRegistry.Builder<SimpleConverterRegistry, Builder> {

        private Map<SerializedDataType, Map<String, Link>> chains = new HashMap<>();

        @Override
        public Builder add(final SerializedDataType type, final String fromVersion, final String toVersion,
                           final Converter<?, ?> converter) {
            Contract.requireArgNotNull("type", type);
            Contract.requireArgNotNull("fromVersion", fromVersion);
            Contract.requireArgNotNull("toVersion", toVersion);
            Contract.requireArgNotNull("converter", converter);
            if (fromVersion.equals(toVersion)) {
                throw new IllegalArgumentException("fromVersion and toVersion must differ, but both were: " + fromVersion);
            }
            final Map<String, Link> chain = chains.computeIfAbsent(type, key -> new HashMap<>());
            if (chain.containsKey(fromVersion)) {
                throw new IllegalArgumentException(
                        "A converter with source version '" + fromVersion + "' is already registered for type '" + type + "'");
            }
            chain.put(fromVersion, new Link(toVersion, converter));
            return this;
        }

        @Override
        public SimpleConverterRegistry build() {
            final Map<SerializedDataType, Map<String, Link>> tmp = chains;
            chains = new HashMap<>();
            return new SimpleConverterRegistry(tmp);
        }

    }

}
