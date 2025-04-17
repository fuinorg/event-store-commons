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
import org.fuin.objects4j.core.KeyValue;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.List;

/**
 * Projection stream identifier that is based on a name.
 */
@Immutable
public final class ProjectionStreamId implements StreamId {

    private static final long serialVersionUID = 1L;

    private final String name;

    /**
     * Constructor for projection.
     *
     * @param name
     *            Unique name.
     */
    public ProjectionStreamId(@NotNull final String name) {
        Contract.requireArgNotNull("name", name);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isProjection() {
        return true;
    }

    @Override
    public <T> T getSingleParamValue() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " has no parameters");
    }

    @Override
    public List<KeyValue> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public String asString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProjectionStreamId other = (ProjectionStreamId) obj;
        return name.equals(other.name);
    }

    @Override
    public String toString() {
        return name;
    }

}
