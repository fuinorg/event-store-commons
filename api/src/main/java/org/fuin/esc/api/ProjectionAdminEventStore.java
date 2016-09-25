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
package org.fuin.esc.api;

import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * An event store that provides a projection administration API.
 */
public interface ProjectionAdminEventStore {

    /**
     * Determines if a projection exists.
     * 
     * @param projectionId Unique identifier of the projection.
     * 
     * @return TRUE if the projection exists, else FALSE.
     */
    public boolean projectionExists(@NotNull StreamId projectionId);
    
    /**
     * Enables an existing projection. If the projection is already enabled, the
     * command is ignored.
     * 
     * @param projectionId
     *            Unique projection identifier.
     * 
     * @throws StreamNotFoundException
     *             The given projection could not be enabled because it does not
     *             exist.
     */
    public void enableProjection(@NotNull StreamId projectionId) throws StreamNotFoundException;

    /**
     * Disables an existing projection. If the projection is already disabled,
     * the command is ignored.
     * 
     * @param projectionId
     *            Unique projection identifier.
     * 
     * @throws StreamNotFoundException
     *             The given projection could not be disabled because it does
     *             not exist.
     */
    public void disableProjection(@NotNull StreamId projectionId) throws StreamNotFoundException;

    /**
     * Creates a new projection that selects an array of events by their type.
     * 
     * @param projectionId
     *            Unique name of the projection to create.
     * @param category
     *            Category of streams to select events from. A category is the
     *            first part of the stream name before any dashes. Example:
     *            "Vendor" is the category for "Vendor-1" and "Vendor-123".
     * @param enable
     *            Enable the projection (<code>true</code>) or not
     *            (<code>false</code>).
     * @param eventType
     *            Unique type names of events to select.
     * 
     * @throws StreamAlreadyExistsException
     *             The given projection could not be created because it already
     *             exists.
     */
    public void createProjection(@NotNull StreamId projectionId, @NotNull StreamId category, boolean enable,
            @NotNull TypeName... eventType) throws StreamAlreadyExistsException;

    /**
     * Creates a new projection that selects a list of events by their type.
     * 
     * @param projectionId
     *            Unique name of the projection to create.
     * @param category
     *            Category of streams to select events from. A category is the
     *            first part of the stream name before any dashes. Example:
     *            "Vendor" is the category for "Vendor-1" and "Vendor-123".
     * @param enable
     *            Enable the projection (<code>true</code>) or not
     *            (<code>false</code>).
     * @param eventTypes
     *            Unique type names of events to select.
     * 
     * @throws StreamAlreadyExistsException
     *             The given projection could not be created because it already
     *             exists.
     */
    public void createProjection(@NotNull StreamId projectionId, @NotNull StreamId category, boolean enable,
            @NotNull List<TypeName> eventTypes) throws StreamAlreadyExistsException;

    /**
     * Deletes an existing projection.
     * 
     * @param projectionId
     *            Projection to delete.
     * 
     * @throws StreamNotFoundException
     *             The given projection could not be deleted because it does not
     *             exist.
     */
    public void deleteProjection(@NotNull StreamId projectionId) throws StreamNotFoundException;

}
