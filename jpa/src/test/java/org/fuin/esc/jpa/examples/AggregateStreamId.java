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
package org.fuin.esc.jpa.examples;

import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.core.KeyValue;
import org.fuin.utils4j.TestOmitted;

import javax.annotation.concurrent.Immutable;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unique name of an aggregate stream.
 */
@TestOmitted("This is only a test class")
@Immutable
public final class AggregateStreamId implements StreamId {

    @Serial
    private static final long serialVersionUID = 1000L;

    private final String type;

    private final String paramName;

    private final String aggregateId;

    private transient List<KeyValue> params;

    /**
     * Constructor with type and id.
     *
     * @param type
     *            Aggregate type.
     * @param paramName
     *            Parameter name.
     * @param aggregateId
     *            Aggregate id.
     */
    public AggregateStreamId(final String type, final String paramName, final String aggregateId) {
        super();
        this.type = type;
        this.paramName = paramName;
        this.aggregateId = aggregateId;
    }

    @Override
    public final String getName() {
        return type;
    }

    @Override
    public final boolean isProjection() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T getSingleParamValue() {
        return (T) aggregateId;
    }

    @Override
    public final List<KeyValue> getParameters() {
        if (params == null) {
            final List<KeyValue> list = new ArrayList<KeyValue>();
            list.add(new KeyValue(paramName, aggregateId));
            params = Collections.unmodifiableList(list);
        }
        return params;
    }

    @Override
    public final String asString() {
        return type + "-" + aggregateId;
    }

    @Override
    public final int hashCode() {
        return asString().hashCode();
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
        final AggregateStreamId other = (AggregateStreamId) obj;
        return asString().equals(other.asString());
    }

    @Override
    public final String toString() {
        return asString();
    }

}
