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
package org.fuin.esc.mem;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of the in-memory projection definitions, shared between the
 * {@link InMemoryEventStoreAsync} (which reads a projection stream by filtering the global event log) and the
 * {@link InMemoryProjectionAdminEventStore} (which creates / enables / disables / deletes projections). A
 * projection is keyed by its name (which equals the target projection-stream name, following the same
 * convention as the JPA backend).
 */
@ThreadSafe
final class InMemoryProjections {

    private final Map<String, InMemoryProjection> byName = new ConcurrentHashMap<>();

    /**
     * Returns whether a projection with the given name exists.
     *
     * @param name Projection / projection-stream name.
     * @return {@literal true} if it exists.
     */
    boolean contains(final String name) {
        return byName.containsKey(name);
    }

    /**
     * Returns the projection with the given name.
     *
     * @param name Projection / projection-stream name.
     * @return Projection or {@literal null} if none exists.
     */
    @Nullable
    InMemoryProjection get(final String name) {
        return byName.get(name);
    }

    /**
     * Adds a new projection.
     *
     * @param name       Unique projection / projection-stream name.
     * @param projection Projection definition.
     */
    void add(final String name, final InMemoryProjection projection) {
        byName.put(name, projection);
    }

    /**
     * Removes a projection.
     *
     * @param name Projection / projection-stream name.
     */
    void remove(final String name) {
        byName.remove(name);
    }

    /**
     * A single projection definition: it selects an event if the event's type name is in {@code eventTypes} or
     * the event carries one of the {@code categoryNames} - the plain-Java equivalent of the JPA SQL filter
     * ({@code type IN (:types) OR categories ∩ (:categories) ≠ ∅}) and of the KurrentDB projection JavaScript.
     */
    @ThreadSafe
    static final class InMemoryProjection {

        private final Set<TypeName> eventTypes;

        private final Set<String> categoryNames;

        private volatile boolean enabled;

        /**
         * Constructor with all data.
         *
         * @param enabled       Whether the projection is enabled (only an enabled projection returns events).
         * @param eventTypes    Type names to select (may be empty).
         * @param categoryNames Categories to select (may be empty).
         */
        InMemoryProjection(final boolean enabled, final Collection<TypeName> eventTypes, final Collection<String> categoryNames) {
            this.enabled = enabled;
            this.eventTypes = Set.copyOf(eventTypes);
            this.categoryNames = Set.copyOf(categoryNames);
        }

        boolean isEnabled() {
            return enabled;
        }

        void enable() {
            this.enabled = true;
        }

        void disable() {
            this.enabled = false;
        }

        /**
         * Returns whether the projection selects nothing at all (no types and no categories).
         *
         * @return {@literal true} if it can never match any event.
         */
        boolean selectsNothing() {
            return eventTypes.isEmpty() && categoryNames.isEmpty();
        }

        /**
         * Returns whether the given event is selected by this projection.
         *
         * @param event Event to test.
         * @return {@literal true} if the event's type is selected or it carries one of the categories.
         */
        boolean matches(final CommonEvent event) {
            return eventTypes.contains(event.getDataType())
                    || !Collections.disjoint(event.getCategories(), categoryNames);
        }

    }

}
