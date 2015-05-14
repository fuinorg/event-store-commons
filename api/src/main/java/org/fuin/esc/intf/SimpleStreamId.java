/**
 * Copyright (C) 2013 Future Invent Informationsmanagement GmbH. All rights
 * reserved. <http://www.fuin.org/>
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
package org.fuin.esc.intf;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Immutable;
import org.fuin.objects4j.vo.KeyValue;

/**
 * Stream identifier that is based on a name.
 */
@Immutable
public final class SimpleStreamId implements StreamId {

    private static final long serialVersionUID = 1L;

    private final String name;

    /**
     * Constructor with name.
     * 
     * @param name
     *            Unique name.
     */
    public SimpleStreamId(@NotNull final String name) {
        Contract.requireArgNotNull("name", name);
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final boolean isProjection() {
        return true;
    }

    @Override
    public final <T> T getSingleParamValue() {
        throw new UnsupportedOperationException(getClass().getSimpleName()
                + " has no parameters");
    }

    @Override
    public final List<KeyValue> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public final String asString() {
        return name;
    }

    @Override
    public final String toString() {
        return name;
    }

    // CHECKSTYLE:OFF Generated code
    
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SimpleStreamId))
            return false;
        SimpleStreamId other = (SimpleStreamId) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    // CHECKSTYLE:ON
    
}
