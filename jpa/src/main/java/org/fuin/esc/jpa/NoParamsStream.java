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
package org.fuin.esc.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.Contract;

/**
 * Contains stream.
 */
@Table(name = "NO_PARAMS_STREAMS")
@Entity
public class NoParamsStream extends JpaStream {

    @Id
    @NotNull
    @Column(name = "STREAM_NAME", nullable = false, updatable = false, length = 100)
    private String streamName;

    /**
     * Protected default constructor for JPA.
     */
    protected NoParamsStream() { //NOSONAR Ignore uninitialized fields
        super();
    }

    /**
     * Constructor with mandatory data.
     * 
     * @param streamId
     *            Unique stream identifier.
     */
    public NoParamsStream(@NotNull final StreamId streamId) {
        super();
        Contract.requireArgNotNull("streamId", streamId);
        this.streamName = streamId.getName();
    }

    @Override
    public JpaStreamEvent createEvent(final StreamId streamId, final JpaEvent eventEntry) {
        return new NoParamsEvent(streamId, incVersion(), eventEntry);
    }

    @Override
    public String toString() {
        return streamName;
    }

}
