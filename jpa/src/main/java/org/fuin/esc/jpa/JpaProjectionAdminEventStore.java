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
import org.fuin.esc.api.ProjectionAdminEventStore;
import org.fuin.esc.api.ProjectionAlreadyExistsException;
import org.fuin.esc.api.ProjectionId;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Relational {@link ProjectionAdminEventStore} that persists projection definitions in the
 * {@code PROJECTIONS} table ({@link JpaProjection}). A projection is a type filter over the global event log:
 * the {@link AbstractJpaEventStore} read paths turn a {@link ProjectionStreamId} into a query that selects all
 * events whose type is part of the projection, ordered by their global insertion order. This is the relational
 * equivalent of an EventStoreDB by-type/category projection, so a catch-up projector (e.g. the cqrs-4-java
 * {@code SpringViewManager} poll loop) works unchanged against the JPA backend.
 * <p>
 * Like the JPA event store it is bound to a single {@link EntityManager} / transaction and therefore not
 * thread safe; give each thread/transaction its own instance.
 * <p>
 * The projection is keyed by the target stream name. This relies on the convention that a view's projection
 * name and its projection stream name are the same string (both default to {@code getName()+"Projection"}), so
 * the id used by {@link #projectionExists(ProjectionId)} and the id used by the read paths resolve to the same
 * row.
 */
@NotThreadSafe
public class JpaProjectionAdminEventStore implements ProjectionAdminEventStore {

    private static final Logger LOG = LoggerFactory.getLogger(JpaProjectionAdminEventStore.class);

    private final EntityManager em;

    /**
     * Constructor with all mandatory data.
     *
     * @param em Entity manager bound to the current transaction.
     */
    public JpaProjectionAdminEventStore(final EntityManager em) {
        super();
        Contract.requireArgNotNull("em", em);
        this.em = em;
    }

    @Override
    public JpaProjectionAdminEventStore open() {
        return this;
    }

    @Override
    public void close() {
        // Nothing to do - the entity manager life cycle is owned by the caller
    }

    @Override
    public boolean projectionExists(final ProjectionId projectionId) {
        Contract.requireArgNotNull("projectionId", projectionId);
        return em.find(JpaProjection.class, projectionId.getName()) != null;
    }

    @Override
    public void createProjection(final ProjectionId projectionId,
                                 final ProjectionStreamId targetStreamId,
                                 final boolean enable,
                                 final List<TypeName> eventTypes,
                                 final List<String> categoryNames) throws ProjectionAlreadyExistsException {
        Contract.requireArgNotNull("projectionId", projectionId);
        Contract.requireArgNotNull("targetStreamId", targetStreamId);
        Contract.requireArgNotNull("eventTypes", eventTypes);
        Contract.requireArgNotNull("categoryNames", categoryNames);

        final String name = targetStreamId.getName();
        if (em.find(JpaProjection.class, name) != null) {
            throw new ProjectionAlreadyExistsException(projectionId);
        }
        final List<String> typeNames = eventTypes.stream().map(TypeName::asBaseType).toList();
        LOG.info("Create projection '{}' selecting events: {} / categories: {}", name, typeNames, categoryNames);
        em.persist(new JpaProjection(name, enable, typeNames, categoryNames));
    }

    @Override
    public void enableProjection(final ProjectionId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        find(projectionId).enable();
    }

    @Override
    public void disableProjection(final ProjectionId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        find(projectionId).disable();
    }

    @Override
    public void deleteProjection(final ProjectionId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        em.remove(find(projectionId));
    }

    private JpaProjection find(final ProjectionId projectionId) throws StreamNotFoundException {
        final JpaProjection projection = em.find(JpaProjection.class, projectionId.getName());
        if (projection == null) {
            throw new StreamNotFoundException(new ProjectionStreamId(projectionId.getName()));
        }
        return projection;
    }

}
