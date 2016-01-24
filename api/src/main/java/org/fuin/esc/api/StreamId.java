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

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.vo.KeyValue;

/**
 * Name of a stream that is unique within the event store.
 */
public interface StreamId extends Serializable {

    /** Uniquely identifies the stream that contains all events. */
    public StreamId ALL = new SimpleStreamId(StreamId.class.getName() + "ALL");

    /**
     * Returns the name of the stream.
     * 
     * @return Unique name.
     */
    @NotNull
    public String getName();

    /**
     * Returns the information if this identifier points to a projection.
     * 
     * @return TRUE if this is an identifier for a projection, else FALSE
     *         (stream).
     */
    public boolean isProjection();

    /**
     * Convenience method that returns the one-and-only parameter value.
     * CAUTION: This method will throw an exception if there are no parameters,
     * more than one parameter or the type of the value cannot be casted to the
     * expected result type.
     * 
     * @return Value of the single parameter.
     * 
     * @param <T>
     *            Type of the returned value.
     */
    @NotNull
    public <T> T getSingleParamValue();

    /**
     * Returns the parameters used in addition to the pure stream name to
     * identify the stream.
     * 
     * @return Ordered unmodifiable list of parameters - May be empty if no
     *         parameters exist.
     */
    @NotNull
    public List<KeyValue> getParameters();

    /**
     * Constructs a unique string from name and parameters.
     * 
     * @return String representation of the identifier.
     */
    @NotNull
    public String asString();

}
