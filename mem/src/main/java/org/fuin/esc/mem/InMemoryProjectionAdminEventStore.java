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

import org.fuin.esc.api.ProjectionAdminEventStore;
import org.fuin.esc.api.ProjectionAlreadyExistsException;
import org.fuin.esc.api.ProjectionId;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.mem.InMemoryProjections.InMemoryProjection;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;

import java.util.List;

/**
 * In-memory {@link ProjectionAdminEventStore} that manages projection definitions expressed as simple Java
 * predicates (event type name and/or category). It shares its {@link InMemoryProjections} registry with an
 * {@link InMemoryEventStoreAsync}, so a created projection is immediately readable as a projection stream
 * through that event store (mem is synchronous - there is no eventual consistency). Obtain an instance via
 * {@link InMemoryEventStoreAsync#getProjectionAdmin()} so admin and reads share the same registry.
 */
@ThreadSafe
public final class InMemoryProjectionAdminEventStore implements ProjectionAdminEventStore {

    private final InMemoryProjections projections;

    private volatile boolean open;

    /**
     * Constructor with the shared registry.
     *
     * @param projections Registry shared with the {@link InMemoryEventStoreAsync}.
     */
    InMemoryProjectionAdminEventStore(final InMemoryProjections projections) {
        this.projections = projections;
    }

    @Override
    public InMemoryProjectionAdminEventStore open() {
        this.open = true;
        return this;
    }

    @Override
    public void close() {
        this.open = false;
    }

    @Override
    public boolean projectionExists(final ProjectionId projectionId) {
        Contract.requireArgNotNull("projectionId", projectionId);
        ensureOpen();
        return projections.contains(projectionId.getName());
    }

    @Override
    public void createProjection(final ProjectionId projectionId, final ProjectionStreamId targetStreamId, final boolean enable,
                                 final List<TypeName> eventTypes, final List<String> categoryNames) throws ProjectionAlreadyExistsException {
        Contract.requireArgNotNull("projectionId", projectionId);
        Contract.requireArgNotNull("targetStreamId", targetStreamId);
        Contract.requireArgNotNull("eventTypes", eventTypes);
        Contract.requireArgNotNull("categoryNames", categoryNames);
        ensureOpen();

        // Keyed by the target stream name (== projection name by convention), matching the JPA backend.
        final String name = targetStreamId.getName();
        if (projections.contains(name)) {
            throw new ProjectionAlreadyExistsException(projectionId);
        }
        projections.add(name, new InMemoryProjection(enable, eventTypes, categoryNames));
    }

    @Override
    public void enableProjection(final ProjectionId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        ensureOpen();
        find(projectionId).enable();
    }

    @Override
    public void disableProjection(final ProjectionId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        ensureOpen();
        find(projectionId).disable();
    }

    @Override
    public void deleteProjection(final ProjectionId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        ensureOpen();
        find(projectionId);
        projections.remove(projectionId.getName());
    }

    private InMemoryProjection find(final ProjectionId projectionId) {
        final InMemoryProjection projection = projections.get(projectionId.getName());
        if (projection == null) {
            throw new StreamNotFoundException(new ProjectionStreamId(projectionId.getName()));
        }
        return projection;
    }

    private void ensureOpen() {
        if (!open) {
            open();
        }
    }

}
