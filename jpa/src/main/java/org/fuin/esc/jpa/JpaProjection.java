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

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.NotThreadSafe;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A projection selects events from the global event log by their type. It has a unique name and a set of
 * event type names that define which events belong to it. As long as it is not enabled, reads return an
 * empty result (the projection is considered "not ready yet").
 */
@NotThreadSafe
@Table(name = "PROJECTIONS")
@Entity
public class JpaProjection {

    @Id
    @NotNull
    @Column(name = "NAME", nullable = false, updatable = false, length = 250)
    private String name;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "PROJECTION_EVENT_TYPES", joinColumns = @JoinColumn(name = "PROJECTION_NAME"))
    @Column(name = "EVENT_TYPE", length = 255, nullable = false)
    private Set<String> eventTypes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "PROJECTION_EVENT_CATEGORIES", joinColumns = @JoinColumn(name = "PROJECTION_NAME"))
    @Column(name = "EVENT_CATEGORY", length = 255, nullable = false)
    private Set<String> categories = new HashSet<>();

    /**
     * Protected default constructor for JPA.
     */
    @SuppressWarnings("NullAway.Init") // Fields are populated by JPA
    protected JpaProjection() { //NOSONAR Ignore uninitialized fields
        super();
    }

    /**
     * Constructor with all mandatory data.
     *
     * @param name
     *            Unique name for the projection.
     */
    public JpaProjection(final String name) {
        super();
        Contract.requireArgNotNull("name", name);
        this.name = name;
    }

    /**
     * Constructor with all data.
     *
     * @param name
     *            Unique name for the projection.
     * @param enabled
     *            FALSE if the query is being created, else TRUE.
     */
    public JpaProjection(final String name, final boolean enabled) {
        super();
        Contract.requireArgNotNull("name", name);
        this.name = name;
        this.enabled = enabled;
    }

    /**
     * Constructor with name, enabled flag and event type filter.
     *
     * @param name
     *            Unique name for the projection.
     * @param enabled
     *            FALSE if the projection is being created, else TRUE.
     * @param eventTypes
     *            Unique type names of the events selected by this projection.
     */
    public JpaProjection(final String name, final boolean enabled, final Collection<String> eventTypes) {
        this(name, enabled, eventTypes, Set.of());
    }

    /**
     * Constructor with name, enabled flag, event type filter and category filter.
     *
     * @param name
     *            Unique name for the projection.
     * @param enabled
     *            FALSE if the projection is being created, else TRUE.
     * @param eventTypes
     *            Unique type names of the events selected by this projection.
     * @param categories
     *            Category names selected by this projection.
     */
    public JpaProjection(final String name, final boolean enabled, final Collection<String> eventTypes,
                         final Collection<String> categories) {
        super();
        Contract.requireArgNotNull("name", name);
        Contract.requireArgNotNull("eventTypes", eventTypes);
        Contract.requireArgNotNull("categories", categories);
        this.name = name;
        this.enabled = enabled;
        this.eventTypes = new HashSet<>(eventTypes);
        this.categories = new HashSet<>(categories);
    }

    /**
     * Returns the unique name of the projection.
     *
     * @return Projection name.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the information if the query is enabled.
     *
     * @return FALSE if the query is being created, else TRUE.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the type names of the events selected by this projection.
     *
     * @return Unmodifiable set of unique event type names.
     */
    @NotNull
    public Set<String> getEventTypes() {
        return new HashSet<>(eventTypes);
    }

    /**
     * Returns the category names selected by this projection.
     *
     * @return Unmodifiable set of category names.
     */
    @NotNull
    public Set<String> getCategories() {
        return new HashSet<>(categories);
    }

    /**
     * Enables the projection (makes reads return its events).
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * Disables the projection (reads return an empty result).
     */
    public void disable() {
        this.enabled = false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof JpaProjection other))
            return false;
        if (name == null) {
            return other.name == null;
        } else return name.equals(other.name);
    }


    @Override
    public String toString() {
        return name;
    }

}
