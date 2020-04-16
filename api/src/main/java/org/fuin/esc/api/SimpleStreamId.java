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
     * Constructor with mandatory data.
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
        return false;
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
        return name;
    }

    @Override
    public final int hashCode() {
        return name.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimpleStreamId other = (SimpleStreamId) obj;
        return name.equals(other.name);
    }

    @Override
    public final String toString() {
        return name;
    }

}
