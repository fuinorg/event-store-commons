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

import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.core.KeyValue;

import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.List;

/**
 * Projection identifier that is based on a name.
 */
@Immutable
public final class ProjectionJpaStreamId implements JpaStreamId {

    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final String nativeTableName;

    /**
     * Constructor with mandatory data.
     *
     * @param entityName
     *            Unique entity name (Simple JPA entity class name).
     * @param nativeTableName
     *            Unique database table name.
     */
    public ProjectionJpaStreamId(@NotNull final String entityName, @NotNull final String nativeTableName) {
        Contract.requireArgNotNull("entityName", entityName);
        Contract.requireArgNotNull("nativeTableName", nativeTableName);
        this.entityName = entityName;
        this.nativeTableName = nativeTableName;
    }

    @Override
    public String getName() {
        return entityName;
    }

    @Override
    public boolean isProjection() {
        return true;
    }

    @Override
    public String getEntityName() {
        return entityName;
    }

    @Override
    public String getNativeTableName() {
        return nativeTableName;
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
        return entityName;
    }

    @Override
    public int hashCode() {
        return entityName.hashCode();
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
        final ProjectionJpaStreamId other = (ProjectionJpaStreamId) obj;
        return entityName.equals(other.entityName);
    }

    @Override
    public String toString() {
        return entityName;
    }

}
