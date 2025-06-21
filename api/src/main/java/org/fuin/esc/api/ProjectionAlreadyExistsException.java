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

import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;

import javax.annotation.concurrent.Immutable;
import java.io.Serial;

/**
 * Signals that a projection with that ID already exist and cannot be created.
 */
@Immutable
public final class ProjectionAlreadyExistsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ProjectionId projectionId;

    /**
     * Constructor with all data.
     *
     * @param projectionId Unique name of the projection.
     */
    public ProjectionAlreadyExistsException(@NotNull final ProjectionId projectionId) {
        super("Stream '" + projectionId + "' already exist");
        Contract.requireArgNotNull("projectionId", projectionId);
        this.projectionId = projectionId;
    }

    /**
     * Returns the unique identifier of the projection.
     *
     * @return Projection that couldn't be created.
     */
    @NotNull
    public ProjectionId getProjectionId() {
        return projectionId;
    }

}
