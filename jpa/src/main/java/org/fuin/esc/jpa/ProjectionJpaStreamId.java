/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.jpa;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Immutable;
import org.fuin.objects4j.vo.KeyValue;

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
    public final String getName() {
        return entityName;
    }

    @Override
    public final boolean isProjection() {
        return true;
    }

    @Override
    public final String getEntityName() {
        return entityName;
    }

    @Override
    public final String getNativeTableName() {
        return nativeTableName;
    }
    
    @Override
    public final <T> T getSingleParamValue() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " has no parameters");
    }

    @Override
    public final List<KeyValue> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public final String asString() {
        return entityName;
    }

    @Override
    public final String toString() {
        return entityName;
    }

    // CHECKSTYLE:OFF Generated code

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityName == null) ? 0 : entityName.hashCode());
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ProjectionJpaStreamId))
            return false;
        ProjectionJpaStreamId other = (ProjectionJpaStreamId) obj;
        if (entityName == null) {
            if (other.entityName != null)
                return false;
        } else if (!entityName.equals(other.entityName))
            return false;
        return true;
    }

    // CHECKSTYLE:ON

}
